package common.java.Http.Client;

import common.java.String.StringHelper;
import common.java.Thread.ThreadHelper;
import common.java.nLogger.nLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WebSocketClient {
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final AtomicBoolean connect_status = new AtomicBoolean(false);

    private static final ConcurrentHashMap<String, WebSocketClient> wsClientCache = new ConcurrentHashMap<>();
    private int reTryMax = 1000;
    private final String ws_url;
    private Channel ch; // 连接管道
    private int reTryDelay = 5000;
    private WebSocketClientHandler handler;
    // 记录各类回调,重连接后接入
    // 接收
    private Consumer<String> onReceive;
    // 重连回调
    private Consumer<ChannelHandlerContext> onReconnected;
    private boolean canQuit = false;

    private WebSocketClient(String ws_url) {
        this.ws_url = ws_url;
    }

    public static WebSocketClient build(String ws_url) {
        WebSocketClient wsc;
        if (wsClientCache.containsKey(ws_url)) {
            wsc = wsClientCache.get(ws_url);
        } else {
            wsc = new WebSocketClient(ws_url);
            wsClientCache.put(ws_url, wsc);
        }
        return wsc;
    }

    private int getPort(URI uri) {
        final int port;
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }
        return port;
    }

    public void reConnect(EventLoopGroup loop, Runnable reCallback) {
        connect_status.set(false);
        // 不能退出，断开重连
        if (!canQuit) {
            if (reCallback != null) {
                reCallback.run();
            }
            this.connect(loop);
        } else {
            // 确定断开连接
            wsClientCache.remove(ws_url);
        }
    }

    // 连接服务器
    public WebSocketClient connect() {
        return connect(group);
    }

    public WebSocketClient connect(EventLoopGroup loop) {
        try {
            URI uri = new URI(ws_url);
            handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
            // 连接成功回调
            handler.setOnAccept(ctx -> {
                // 获得新连接通道
                ch = handler.handshakeFuture().channel();

                // 恢复接收回调
                if (onReceive != null) {
                    handler.setOnReceive(onReceive);
                }
                // 设置连接状态
                connect_status.set(true);
            });
            // 网络中断回调
            handler.setOnDisconnected(ctx -> this.reConnect(ctx.channel().eventLoop(), () -> {
                // 处理重连接回调
                if (onReconnected != null) {
                    onReconnected.accept(ctx);
                }
            }));
            // 连接服务器
            _connect(uri, handler, loop);
        } catch (Exception e) {
            nLogger.errorInfo(e);
        }
        return this;
    }

    public WebSocketClient syncConnect() {
        connect();
        while (!connect_status.compareAndExchange(true, false)) {
            ThreadHelper.sleep(100);
        }
        ch = handler.handshakeFuture().channel();
        return this;
    }

    public Channel getCh() {
        return ch;
    }

    private boolean _connect(URI uri, WebSocketClientHandler _handle, EventLoopGroup loop) {
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        final int port = getPort(uri);
        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return false;
        }
        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        try {
            if (ssl) {
                sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } else {
                sslCtx = null;
            }
            ChannelInitializer<SocketChannel> chInit = new ChannelInitializer<>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                    }
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), _handle);
                }
            };
            Bootstrap b = new Bootstrap();
            b.group(loop).channel(NioSocketChannel.class).handler(chInit);
            try {
                // 设置连接socket通道
                b.connect(uri.getHost(), port).addListener(new WebSocketConnectionListener(this));
                // 设置不可推出
                canQuit = false;
                return true;
            } catch (Exception e) {
                nLogger.logInfo(e);
            }
        } catch (SSLException e) {
            nLogger.logInfo(e);
        }
        return false;
    }

    // 发送数据
    public WebSocketClient send(Object msg) {
        WebSocketFrame frame = new TextWebSocketFrame(StringHelper.toString(msg));
        ch.writeAndFlush(frame);
        return this;
    }

    // 接收数据
    public WebSocketClient onReceive(Consumer<String> fn) {
        onReceive = fn;
        handler.setOnReceive(onReceive);
        return this;
    }

    // 重连回调设置
    public WebSocketClient onReconnect(Consumer<ChannelHandlerContext> fn) {
        onReconnected = fn;
        return this;
    }

    // 关闭客户端
    public void close() {
        canQuit = true;
        if (ch != null) {
            ch.close();
            ch = null;
        }
    }

    public int getReTryMax() {
        return reTryMax;
    }

    public void setReTryMax(int reTryMax) {
        this.reTryMax = reTryMax;
    }

    public int getReTryDelay() {
        return reTryDelay;
    }

    public void setReTryDelay(int reTryDelay) {
        this.reTryDelay = reTryDelay;
    }
}
