package common.java.File;


import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

public class FileText extends FileHelper<FileText> {
    private FileText(File file, Charset charset) {
        super(file, charset);
    }

    public static FileText build(String filePath) {
        return new FileText(new File(filePath), Charset.defaultCharset());
    }

    public static FileText build(String filePath, Charset charsetName) {
        return build(new File(filePath), charsetName);
    }

    public static FileText build(File file) {
        return new FileText(file, Charset.defaultCharset());
    }

    public static FileText build(File file, Charset charsetName) {
        return new FileText(file, charsetName);
    }

    public File file() {
        return super.file;
    }

    // new OutputStreamWriter(new FileOutputStream(new File(this.file)),"utf-8")
    public boolean write(String in) {
        boolean rb = true;
        // try (FileWriter write = new FileWriter(this.file)) {
        try (var write = new OutputStreamWriter(new FileOutputStream(this.file), this.charset.name())) {
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
        try (var write = new OutputStreamWriter(new FileOutputStream(this.file), this.charset.name())) {
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
        try (var write = new OutputStreamWriter(new FileOutputStream(this.file, true), this.charset.name())) {
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
        try (var write = new OutputStreamWriter(new FileOutputStream(this.file, true), this.charset.name())) {
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
        try (var write = new OutputStreamWriter(new FileOutputStream(this.file, true), this.charset.name())) {
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
        try (var read = new InputStreamReader(new FileInputStream(this.file), this.charset.name())) {
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
        try (var read = new InputStreamReader(new FileInputStream(this.file), this.charset.name())) {
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
