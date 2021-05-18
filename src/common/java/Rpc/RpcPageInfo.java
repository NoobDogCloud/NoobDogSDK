package common.java.Rpc;

import org.json.gsc.JSONArray;

public class RpcPageInfo {
    private final int idx;
    private final int max;
    private final long count;
    private final JSONArray info;

    private RpcPageInfo(int idx, int max, long count, JSONArray info) {
        this.idx = idx;
        this.max = max;
        this.count = count;
        this.info = info;
    }

    public static RpcPageInfo Instant(int idx, int max, long count, JSONArray info) {
        return new RpcPageInfo(idx, max, count, info);
    }

    public String toString() {
        return rMsg.netPAGE(idx, max, count, info);
    }
}
