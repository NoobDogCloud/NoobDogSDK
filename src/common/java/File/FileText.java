package common.java.File;

import org.apache.commons.codec.Charsets;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

public class FileText extends FileHelper<FileText> {
    private final Charset charset;

    private FileText(File file) {
        super(file);
        this.charset = Charsets.toCharset("utf8");
    }

    private FileText(File file, Charset charset) {
        super(file);
        this.charset = charset;
    }

    public static FileText build(String filePath, Charset charset) {
        return new FileText(new File(filePath), charset);
    }

    public static FileText build(File file, Charset charset) {
        return new FileText(file, charset);
    }

    public static FileText build(String filePath) {
        return new FileText(new File(filePath));
    }

    public static FileText build(File file) {
        return new FileText(file);
    }

    public File file() {
        return super.file;
    }

    public boolean write(String in) {
        boolean rb = true;
        try (FileWriter write = new FileWriter(this.file)) {
            try (BufferedWriter bw = new BufferedWriter(write)) {
                bw.write(in);
                bw.flush();
            } catch (Exception e) {
                error_handle();
                rb = false;
            }
        } catch (Exception e) {
            error_handle();
            rb = false;
        }
        return rb;
    }

    public boolean write(List<String> in) {
        boolean rb = true;
        try (FileWriter write = new FileWriter(this.file)) {
            try (BufferedWriter bw = new BufferedWriter(write)) {
                for (String line : in) {
                    bw.newLine();
                    bw.write(line);
                }
                bw.flush();
            } catch (Exception e) {
                error_handle();
                rb = false;
            }
        } catch (Exception e) {
            error_handle();
            rb = false;
        }
        return rb;
    }

    public FileText append(String in) {
        try (FileWriter write = new FileWriter(this.file, true)) {
            try (BufferedWriter bw = new BufferedWriter(write)) {
                bw.write(in);
                bw.flush();
            } catch (Exception e) {
                error_handle();
            }
        } catch (Exception e) {
            error_handle();
        }
        return this;
    }

    public FileText appendLine(String in) {
        try (FileWriter write = new FileWriter(this.file, true)) {
            try (BufferedWriter bw = new BufferedWriter(write)) {
                bw.newLine();
                bw.write(in);
                bw.flush();
            } catch (Exception e) {
                error_handle();
            }
        } catch (Exception e) {
            error_handle();
        }
        return this;
    }

    public FileText append(List<String> in) {
        try (FileWriter write = new FileWriter(this.file, true)) {
            try (BufferedWriter bw = new BufferedWriter(write)) {
                for (String line : in) {
                    bw.newLine();
                    bw.write(line);
                }
                bw.flush();
            } catch (Exception e) {
                error_handle();
            }
        } catch (Exception e) {
            error_handle();
        }
        return this;
    }

    public Stream<String> read() {
        try (FileReader read = new FileReader(this.file)) {
            try (BufferedReader bw = new BufferedReader(read)) {
                return bw.lines();
            } catch (Exception e) {
                error_handle();
            }
        } catch (Exception e) {
            error_handle();
        }
        return null;
    }

    public String readString() {
        StringBuilder sb = new StringBuilder();
        try (FileReader read = new FileReader(this.file)) {
            try (BufferedReader bw = new BufferedReader(read)) {
                Stream<String> rArray = bw.lines();
                rArray.forEach(sb::append);
            } catch (Exception e) {
                error_handle();
            }
        } catch (Exception e) {
            error_handle();
        }
        return sb.toString();
    }
}
