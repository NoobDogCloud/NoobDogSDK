package common.java.HttpServer.Upload;

import common.java.HttpServer.HttpContext;
import common.java.nLogger.nLogger;
import io.netty.buffer.ByteBuf;
import org.json.gsc.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UploadFile {
    private final UploadFileInfo file;

    public UploadFile(UploadFileInfo file) {
        this.file = file;
    }

    public static List<UploadFile> getAll() {
        List<UploadFile> fileList = new ArrayList<>();
        JSONObject formData = HttpContext.current().parameter();
        if (formData != null) {
            for (String key : formData.keySet()) {
                Object obj = formData.get(key);
                if (obj instanceof UploadFileInfo) {
                    fileList.add(new UploadFile((UploadFileInfo) obj));
                }
            }
        }
        return fileList;
    }

    public UploadFileInfo getFileInfo() {
        return file;
    }

    public boolean writeTo(String filePath) {
        return writeTo(new File(filePath));
    }

    public boolean writeTo(File diskFile) {
        boolean rb = false;
        if (file.isBuff()) {
            FileOutputStream fin = null;
            try {
                fin = new FileOutputStream(diskFile);
                ByteBuf buff = file.getLocalBytes();
                buff.readBytes(fin, buff.readableBytes());
                rb = true;
            } catch (FileNotFoundException e) {
                nLogger.debugInfo(e, diskFile.getAbsolutePath() + "文件不存在");
            } catch (IOException e) {
                nLogger.debugInfo(e, diskFile.getAbsolutePath() + "文件IO失败");
            } finally {
                try {
                    Objects.requireNonNull(fin).close();
                } catch (IOException e) {
                    nLogger.debugInfo(e, diskFile.getAbsolutePath() + "文件关闭失败");
                }
            }
        } else {
            File src = file.getLocalFile();
            if (diskFile.exists()) {
                diskFile.delete();
            }
            src.renameTo(diskFile);
            rb = true;
        }
        return rb;
    }
}
