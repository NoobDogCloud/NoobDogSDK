package common.java.DataSource.AsyncProgress;

public record ProgressInfo(String message, int type) {

    public static ProgressInfo build(String message) {
        return new ProgressInfo(message, 0);
    }

    public static ProgressInfo build(String message, int type) {
        return new ProgressInfo(message, type);
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    static class InfoType {
        public static final int INFO = 0;
        public static final int ERROR = 1;
        public static final int WARNING = 2;
        public static final int SUCCESS = 3;
    }
}
