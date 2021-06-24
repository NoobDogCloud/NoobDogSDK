package common.java.Http.Client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

public class WebSocketConnectionListener implements ChannelFutureListener {
    private final WebSocketClient wsc;

    public WebSocketConnectionListener(WebSocketClient wsc) {
        this.wsc = wsc;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess()) {
            System.out.println("Reconnect Gsc Center Server ...");
            final EventLoop loop = channelFuture.channel().eventLoop();
            loop.schedule(() -> wsc.reConnect(loop), 10L, TimeUnit.SECONDS);
        }
    }
}
