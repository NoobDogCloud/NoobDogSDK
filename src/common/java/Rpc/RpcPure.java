package common.java.Rpc;

import common.java.String.StringHelper;

public class RpcPure {
    private final Object payload;

    private RpcPure(Object payload) {
        this.payload = payload;
    }

    public static RpcPure Instant(Object payload) {
        return new RpcPure(payload);
    }

    public String toString() {
        return StringHelper.toString(this.payload);
    }

    public Object payload() {
        return payload;
    }
}
