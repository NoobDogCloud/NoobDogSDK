package common.java.Rpc;


import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class rMsg {
    public static JSONObject netMSG(Object state, Object data) {
        return rMsgJson.build().netMSG(state, data);
    }

    public static JSONObject netMSG(Object data) {
        return rMsgJson.build().netMSG(data);
    }

    public static JSONObject netMSG(int state, String message, Object data) {
        return rMsgJson.build().netMSG(state, message, data);
    }

    public static JSONObject netPAGE(int idx, int max, long count, JSONArray record) {
        return rMsgJson.build().netPAGE(idx, max, count, record);
    }

    public static JSONObject netState(Object state) {
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
