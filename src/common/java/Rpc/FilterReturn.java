package common.java.Rpc;

public record FilterReturn(boolean state, String msg) {

    public static FilterReturn buildTrue() {
        return new FilterReturn(true, "");
    }

    public static FilterReturn build(boolean state, String msg) {
        return new FilterReturn(state, msg);
    }

    public static FilterReturn success() {
        return new FilterReturn(true, "");
    }

    public static FilterReturn fail(String msg) {
        return new FilterReturn(false, msg);
    }


    public boolean isSuccess() {
        return state;
    }

    public String message() {
        return msg;
    }
}
