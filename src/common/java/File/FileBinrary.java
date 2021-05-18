package common.java.File;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.MappedByteBuffer;

/**
 * 不用本类时,一定要手动调用 release
 */
public class FileBinrary extends FileHelper<FileBinrary> {

    private FileBinrary(File file) {
        super(file);
    }

    public static FileBinrary build(String filePath) {
        return new FileBinrary(new File(filePath));
    }

    public static FileBinrary build(File file) {
        return new FileBinrary(file);
    }

    public ByteBuf slice(long offset, int length) {
        MappedByteBuffer[] mapArray = getFileBuffer(offset, length);
        int currentOffset = (int) offset & MAX_BLOCK_LENGTH;
        int currentLength = MAX_BLOCK_LENGTH - currentOffset;
        MappedByteBuffer fmap = mapArray[0];
        // wrap 文件内存映射
        ByteBuf bs = Unpooled.wrappedBuffer(fmap.array());
        // 复制头块
        ByteBuf rbuff = Unpooled.wrappedBuffer(bs.slice(currentOffset, currentLength));
        // 如果包含第二块，复制尾块
        if (mapArray.length > 1) {
            fmap = mapArray[1];
            bs = Unpooled.wrappedBuffer(fmap.array());
            rbuff = Unpooled.wrappedBuffer(rbuff, Unpooled.wrappedBuffer(bs.slice(0, MAX_BLOCK_LENGTH - currentLength)));
        }
        return rbuff;
    }

    public ByteBuf read(int length) {
        ByteBuf buff = PooledByteBufAllocator.DEFAULT.buffer(length);
        try {
            super.getInputStream().read(buff.array());
        } catch (Exception e) {
            error_handle();
            buff = null;
        }
        return buff;
    }

    public boolean write(ByteBuf in, int length) {
        try {
            super.getOutputStream().write(in.array());
            return true;
        } catch (Exception e) {
            error_handle();
            return false;
        }
    }

    public FileBinrary append(ByteBuf in) {
        try (FileOutputStream fos = new FileOutputStream(this.file, true)) {
            fos.write(in.array());
            fos.flush();
        } catch (Exception e) {
            error_handle();
        }
        return this;
    }
}
