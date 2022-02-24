package common.java.Rpc;

public record RpcMessage(int errorCode, String msg) {

    public static RpcMessage Instant(int errorCode, String msg) {
        return new RpcMessage(errorCode, msg);
    }

    public static RpcMessage Instant(boolean state, String msg) {
        return new RpcMessage(state ? 0 : 1, msg);
    }

    public static RpcMessage Instant(FilterReturn fReturn) {
        return new RpcMessage(fReturn.state() ? 0 : 1, fReturn.message());
    }

    public String toString() {
        return rMsg.netMSG(errorCode, msg, "").toString();
    }
}
