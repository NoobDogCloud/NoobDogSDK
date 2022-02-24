package common.java.Http.Server.Upload;

import common.java.File.FileBinary;
import common.java.File.FileHelper;
import io.netty.buffer.ByteBuf;

import java.io.File;

public class UploadFileInfo {
    private final long maxLen;
    private String clientName;
    private File localFile;
    private String mime;
    private ByteBuf _content;
    private boolean isBuff;

    public UploadFileInfo() {
        isBuff = false;
        maxLen = 0;
    }

    public UploadFileInfo(String orgName, String type, long max) {
        clientName = orgName;
        mime = type;
        maxLen = max;
        isBuff = true;
    }

    public String getClientName() {
        return clientName;
    }

    public File getLocalFile() {
        // 如果本地文件没生成，那么就生成一个
        if (localFile == null) {
            File file = new File(FileHelper.buildTempFile());
            if (FileBinary.build(file).write(_content)) {
                localFile = file;
            }
        }
        return localFile;
    }

    public ByteBuf getLocalBytes() {
        return _content;
    }

    public Object getContent() {
        return isBuff ? _content : localFile;
    }

    public String getFileType() {
        return mime;
    }

    public long getFileSize() {
        return maxLen;
    }

    public UploadFileInfo append(File local) {
        localFile = local;
        isBuff = false;
        return this;
    }

    public UploadFileInfo append(ByteBuf content) {
        _content = content;
        isBuff = true;
        return this;
    }

    public boolean isBuff() {
        return isBuff;
    }
}
