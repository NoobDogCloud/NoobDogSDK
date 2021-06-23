package common.java.Http.Server.Upload;

import io.netty.buffer.ByteBuf;

import java.io.File;

public class UploadFileInfo {
    private final long maxlen;
    private String clientName;
    private File localFile;
    private String mime;
    private ByteBuf _content;
    private boolean isbuff;

    public UploadFileInfo() {
        isbuff = false;
        maxlen = 0;
    }

    public UploadFileInfo(String orgName, String type, long max) {
        clientName = orgName;
        mime = type;
        maxlen = max;
        isbuff = true;
    }

    public String getClientName() {
        return clientName;
    }

    public File getLocalFile() {
        return localFile;
    }

    public ByteBuf getLocalBytes() {
        return _content;
    }

    public Object getContent() {
        return isbuff ? _content : localFile;
    }

    public String getFileType() {
        return mime;
    }

    public long getFileSize() {
        return maxlen;
    }

    public UploadFileInfo append(File local) {
        localFile = local;
        isbuff = false;
        return this;
    }

    public UploadFileInfo append(ByteBuf content) {
        _content = content;
        isbuff = true;
        return this;
    }

    public boolean isBuff() {
        return isbuff;
    }
}
