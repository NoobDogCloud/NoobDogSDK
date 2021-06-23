package common.java.Http.Server;

import common.java.nLogger.nLogger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.nio.charset.StandardCharsets;

//public class WSText extends SimpleChannelInboundHandler<TextWebSocketFrame>{
public class WSText extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // TODO Auto-generated method stub
        WebSocketFrame frame = (WebSocketFrame) msg;
        ByteBuf buf = frame.content();  //真正的数据是放在buf里面的
        String aa = buf.toString(StandardCharsets.UTF_8);  //将数据按照utf-8的方式转化为字符串
        nLogger.debugInfo(aa);
        WebSocketFrame out = new TextWebSocketFrame(aa);  //创建一个websocket帧，将其发送给客户端
        ctx.pipeline().writeAndFlush(out).addListener((ChannelFutureListener) future -> {
            // TODO Auto-generated method stub
            ctx.pipeline().close();  //从pipeline上面关闭的时候，会关闭底层的chanel，而且会从eventloop上面取消注册
        });
    }
}
