package common.java.Check;

public class CheckResult {
    private final boolean status;
    private final String message;

    private CheckResult(boolean status, String message) {
        this.status = status;
        this.message = message;
    }

    public static CheckResult build(boolean status, String message) {
        return new CheckResult(status, message);
    }

    public static CheckResult buildTrue() {
        return new CheckResult(true, "");
    }

    public boolean isStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
