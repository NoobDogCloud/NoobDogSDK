package common.java.Rpc;

import org.json.gsc.JSONArray;

public class rMsgString {
    private final rMsgJson info;

    private rMsgString() {
        info = rMsgJson.build();
    }

    public static rMsgString build() {
        return new rMsgString();
    }

    public String netMSG(Object state, Object data) {
        return info.netMSG(state, data).toString();
    }

    public String netMSG(Object data) {
        return info.netMSG(data).toString();
    }

    public String netMSG(int state, String message, Object data) {
        return info.netMSG(state, message, data).toString();
    }

    public String netPAGE(int idx, int max, long count, JSONArray record) {
        return info.netPAGE(idx, max, count, record).toString();
    }

    public String netState(Object state) {
        return info.netState(state).toString();
    }
}
