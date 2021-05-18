package common.java.Coordination.Common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class payPacket {
    public final int length;
    private final ByteBuf buf;
    private int need_length;

    private payPacket(int length) {
        this.length = length;
        this.need_length = length - 8;    // 需要的字节要减去标志位和长度本身
        this.buf = Unpooled.directBuffer();
    }

    public static payPacket build(int length) {
        return new payPacket(length);
    }

    public ByteBuf getBuffer() {
        return this.buf;
    }

    private boolean _append(ByteBuf buffer, int len) {
        buf.writeBytes(buffer, len);
        // buf.discardReadBytes();
        need_length -= len;
        return need_length == 0;
    }

    // 是否满了
    public boolean append(ByteBuf buffer) {
        int read_length = buffer.readableBytes();
        return _append(buffer, Math.min(need_length, read_length));
    }
}
