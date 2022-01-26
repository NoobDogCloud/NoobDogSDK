package common.java.Rpc;

public record RpcJsonFilterBlock(FilterJsonCallback callback, boolean required,
                                 String message) {

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