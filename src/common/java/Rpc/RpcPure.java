package common.java.Rpc;

import common.java.String.StringHelper;

public record RpcPure(Object payload) {

    public static RpcPure Instant(Object payload) {
        return new RpcPure(payload);
    }

    public String toString() {
        return StringHelper.toString(this.payload);
    }
}
