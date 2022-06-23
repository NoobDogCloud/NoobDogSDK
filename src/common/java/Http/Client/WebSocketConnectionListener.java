package common.java.Http.Client;

import common.java.nLogger.nLogger;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

public record WebSocketConnectionListener(
        WebSocketClient wsc) implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess()) {
            nLogger.warnInfo("Reconnect Gsc Center Server ...");
            final EventLoop loop = channelFuture.channel().eventLoop();
            loop.schedule(() -> wsc.reConnect(loop, null), 10L, TimeUnit.SECONDS);
        }
    }
}
