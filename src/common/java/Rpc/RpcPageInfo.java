package common.java.Rpc;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public record RpcPageInfo(int idx, int max, long count, JSONArray<JSONObject> info) {

    public static RpcPageInfo Instant(int idx, int max, long count, JSONArray<JSONObject> info) {
        return new RpcPageInfo(idx, max, count, info);
    }

    public String toString() {
        return rMsg.netPAGE(idx, max, count, info).toString();
    }
}
