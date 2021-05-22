package common.java.Rpc;


import org.json.gsc.JSONArray;

public class rMsg {
    public static String netMSG(Object state, Object data) {
        return rMsgJson.build().netMSG(state, data).toString();
    }
    public static String netMSG(Object data) {
        return rMsgJson.build().netMSG(data).toString();
    }

    public static String netMSG(int state, String message, Object data) {
        return rMsgJson.build().netMSG(state, message, data).toString();
    }

    public static String netPAGE(int idx, int max, long count, JSONArray record) {
        return rMsgJson.build().netPAGE(idx, max, count, record).toString();
    }

    public static String netState(Object state) {
        return netMSG(state, "");
    }

    public static rMsgString asString() {
        return rMsgString.build();
    }

    public static rMsgJson asJson() {
        return rMsgJson.build();
    }

    public static rMsgResponse asResponse() {
        return rMsgResponse.build();
    }
}
