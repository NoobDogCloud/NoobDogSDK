package common.java.Rpc;

public record FilterReturn(boolean state, String msg) {

    public static FilterReturn buildTrue() {
        return new FilterReturn(true, "");
    }

    public static FilterReturn build(boolean state, String msg) {
        return new FilterReturn(state, msg);
    }

    public String message() {
        return msg;
    }
}
