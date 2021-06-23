package common.java.Http.Client;

import common.java.nLogger.nLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;
import java.util.function.Consumer;

public class WebSocketClient {
    private static final EventLoopGroup group = new NioEventLoopGroup();
    private final String ws_url;
    private final Bootstrap b = new Bootstrap();
    private Channel ch; // 连接管道
    private WebSocketClientHandler handler;
    // 重连回调
    private Consumer<Channel> onReconnected;

    private WebSocketClient(String ws_url) {
        this.ws_url = ws_url;
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

    // 连接服务器
    public void connect() {
        try {
            URI uri = new URI(ws_url);
            handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
            // 网络中断回调
            handler.setOnDisconnected(ctx -> {
                this.connect();
            });
            // 连接服务器
            _connect(uri, handler);
            handler.handshakeFuture().sync();
        } catch (Exception e) {
            nLogger.errorInfo(e);
        }
    }

    private boolean _connect(URI uri, WebSocketClientHandler _handle) {
        try {
            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
            final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
            final int port = getPort(uri);
            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                System.err.println("Only WS(S) is supported.");
                return false;
            }
            final boolean ssl = "wss".equalsIgnoreCase(scheme);
            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } else {
                sslCtx = null;
            }
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                    }
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), _handle);
                }
            });
            // 设置连接socket通道
            ch = b.connect(uri.getHost(), port).sync().channel();
            // 连接成功，执行连接成功回调
            if (onReconnected != null) {
                onReconnected.accept(ch);
            }
            return true;
        } catch (Exception e) {
            nLogger.logInfo(e);
            return false;
        }
    }

    // 发送数据
    public WebSocketClient send(Object msg) {
        ch.writeAndFlush(msg);
        return this;
    }

    // 接收数据
    public WebSocketClient onReceive(Consumer<String> fn) {
        handler.setOnReceive(fn);
        return this;
    }

    // 重连回调设置
    public WebSocketClient onReconnect(Consumer<Channel> fn) {
        onReconnected = fn;
        return this;
    }

    // 关闭客户端
    public void close() {
        if (ch != null) {
            ch.close();
            ch = null;
        }
    }
}
