package common.java.Coordination.Common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessagePacketEncoder extends MessageToByteEncoder<GscCenterPacket> {
    private static final int max_length = 1024;

    public MessagePacketEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, GscCenterPacket msg, ByteBuf out) {
        // 分块发送 最大1次1K
        /*
        ByteBuf bArr = msg.toByteBuf();
        ByteBuf pArr;
        int i, l, _i;
        out.discardReadBytes();
        for(i =0, _i =0, l =bArr.readableBytes(); i< l ; ){
            _i+=max_length;
            if( _i > l ){
                int c = max_length - (_i - l);   // 剩余未读字节
                pArr = bArr.slice(i, c);
            }
            else{
                pArr = bArr.slice(i, max_length);
            }
            out.writeBytes(pArr);
            i = _i;
        }
        */
        out.discardReadBytes().writeBytes(msg.toByteBuf());
    }
}