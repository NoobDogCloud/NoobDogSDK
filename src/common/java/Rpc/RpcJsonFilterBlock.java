package common.java.Rpc;

public class RpcJsonFilterBlock {
    private final FilterJsonCallback callback;
    private final String message;
    private final boolean required;

    private RpcJsonFilterBlock(FilterJsonCallback callback, boolean required, String message) {
        this.callback = callback;
        this.message = message;
        this.required = required;
    }

    public static RpcJsonFilterBlock build(FilterJsonCallback callback, String message) {
        return new RpcJsonFilterBlock(callback, true, message);
    }

    public static RpcJsonFilterBlock build(FilterJsonCallback callback) {
        return new RpcJsonFilterBlock(callback, true, "");
    }

    public static RpcJsonFilterBlock build(FilterJsonCallback callback, boolean required, String message) {
        return new RpcJsonFilterBlock(callback, required, message);
    }

    public FilterJsonCallback getCallback() {
        return callback;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRequired() {
        return required;
    }
}