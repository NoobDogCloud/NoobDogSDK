package common.java.File;


import java.io.*;
import java.util.List;
import java.util.stream.Stream;

public class FileText extends FileHelper<FileText> {

    private FileText(File file) {
        super(file);
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
