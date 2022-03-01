package common.java.DataSource.AsyncProgress;

public class ProgressInfo {
    private final String message;
    private final int type; // 0: info, 1: error, 2: warning 3: success

    private ProgressInfo(String message, int type) {
        this.message = message;
        this.type = type;
    }

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

    class InfoType {
        public static final int INFO = 0;
        public static final int ERROR = 1;
        public static final int WARNING = 2;
        public static final int SUCCESS = 3;
    }
}
