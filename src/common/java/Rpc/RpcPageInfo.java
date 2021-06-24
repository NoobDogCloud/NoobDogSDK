package common.java.Rpc;

import org.json.gsc.JSONArray;

public record RpcPageInfo(int idx, int max, long count, JSONArray info) {

    public static RpcPageInfo Instant(int idx, int max, long count, JSONArray info) {
        return new RpcPageInfo(idx, max, count, info);
    }

    public String toString() {
        return rMsg.netPAGE(idx, max, count, info);
    }
}
