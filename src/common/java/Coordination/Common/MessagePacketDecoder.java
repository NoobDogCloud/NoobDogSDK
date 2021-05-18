package common.java.Coordination.Common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MessagePacketDecoder extends ByteToMessageDecoder {
    private final ConcurrentHashMap<String, payPacket> ctxMap;

    public MessagePacketDecoder(ConcurrentHashMap<String, payPacket> ctxMap) {
        this.ctxMap = ctxMap;
    }

    private int getPacketLength(ByteBuf buffer) {
        byte[] sign_byte = new byte[4];
        buffer.readBytes(sign_byte);
        String sign_str = new String(sign_byte);
        // 当前块起始是包标记
        if (!sign_str.equals("GSC_")) {
            return 0;
        }
        // 获得包字节长度(获得下一个包入口地址)
        return buffer.readInt();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        int bag_length;
        int readable_length;
        String channel_name = ctx.channel().id().asLongText();
        for (readable_length = buffer.readableBytes(); readable_length > 0; readable_length = buffer.readableBytes()) {
            bag_length = getPacketLength(buffer);   // 获得包长度,0时可能是某个后续包,也可能是垃圾包
            if (bag_length > readable_length) {     // 包超过当前Buffer
                payPacket payload = payPacket.build(bag_length);
                payload.append(buffer);     // 写入当前数据流除去长度和符号后所有流数据
                ctxMap.put(channel_name, payload);
                break;
            } else {
                if (bag_length == 0) {  // 包长度为0,包含前置数据时,是尾包,否则是垃圾包
                    if (ctxMap.containsKey(channel_name)) {     // 是分块数据,包含前置数据
                        payPacket payload = ctxMap.get(channel_name);
                        buffer.resetReaderIndex();              // 因为试图读了4字节做判断,需要重置
                        if (!payload.append(buffer)) {          // 还有数据没写入
                            continue;
                        }
                        // 包数据拼装完整了,删除频道对应预存数据
                        ctxMap.remove(channel_name);
                        out.add(GscCenterPacket.build(payload.getBuffer()));
                    } else {
                        // buffer.release();
                        break;
                    }
                } else {   // 包小于当前buffer,直接用当前包
                    out.add(GscCenterPacket.build(buffer));
                }
            }
            buffer.discardReadBytes();
        }
    }
}