package common.java.Check;

public record CheckResult(boolean status, String message) {

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
