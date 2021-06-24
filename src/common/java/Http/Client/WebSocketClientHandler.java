package common.java.Http.Client;

import common.java.Thread.ThreadHelper;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;

import java.util.function.Consumer;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;

    private ChannelPromise handshakeFuture;
    // 收到数据回调
    private Consumer<String> onReceive;
    // 连接成功
    private Consumer<ChannelHandlerContext> onAccept;
    // 断线回调
    private Consumer<ChannelHandlerContext> onDisconnected;
    // 关闭回调
    private Consumer<ChannelHandlerContext> onClosed;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public void setOnReceive(Consumer<String> onReceive) {
        this.onReceive = onReceive;
    }

    public void setOnDisconnected(Consumer<ChannelHandlerContext> onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public void setOnClosed(Consumer<ChannelHandlerContext> onClosed) {
        this.onClosed = onClosed;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // System.out.println("handlerAdded");
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // System.out.println("channelActive");
        handshaker.handshake(ctx.channel());
    }

    private void channelReconnect(ChannelHandlerContext ctx) {
        if (onDisconnected != null) {   // 断开时调用上层断开回调，如果是意外断开，执行重新连接
            onDisconnected.accept(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // System.out.println("WebSocket Client 连接正常断开!");
        channelReconnect(ctx);
        ctx.fireChannelInactive();
    }

    public Channel waitAccept() throws Exception {
        while (handshakeFuture == null) {
            ThreadHelper.sleep(100);
        }
        return handshakeFuture.sync().channel();
    }

    public void setOnAccept(Consumer<ChannelHandlerContext> onAccept) {
        this.onAccept = onAccept;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // System.out.println("channelRead0");
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                // System.out.println("WebSocket Client connected!");
                handshakeFuture.setSuccess();
                // 自己唤起阻塞
                if (onAccept != null) {
                    onAccept.accept(ctx);
                }
            } catch (WebSocketHandshakeException e) {
                // System.out.println("WebSocket Client failed to connect");
                handshakeFuture.setFailure(e);
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            throw new IllegalStateException("Unexpected FullHttpResponse");
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame textFrame) {
            // 收到返回数据
            if (onReceive != null) {
                onReceive.accept(textFrame.text());
            }
            // System.out.println("WebSocket Client received message: " + textFrame.text());
        } else if (frame instanceof PongWebSocketFrame) {
            // System.out.println("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            // System.out.println("WebSocket Client received closing");
            if (onClosed != null) {
                onClosed.accept(ctx);
            }
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        if (onClosed != null) {
            onClosed.accept(ctx);
        }
        ctx.close();
        ctx.disconnect();

        // ctx.fireChannelInactive();
        // System.out.println("WebSocket Client 连接异常断开!");
    }
}