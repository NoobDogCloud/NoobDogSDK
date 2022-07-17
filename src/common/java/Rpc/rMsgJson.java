package common.java.Rpc;

import common.java.Number.NumberHelper;
import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.List;

public class rMsgJson {
    private final JSONObject info;

    public rMsgJson() {
        info = JSONObject.build();
    }

    public static rMsgJson build() {
        return new rMsgJson();
    }

    public JSONObject netMSG(Object state, Object data) {
        return netMSG(NumberHelper.number2int(state), "", data);
    }

    public JSONObject netMSG(Object data) {
        if (data == null) {
            return netState(false);
        }

        if (data instanceof Boolean b) {
            return netState(b);
        }

        if (data instanceof List<?>) {
            data = JSONArray.build().put((List<?>) data);
        } else if (data instanceof HashMap<?, ?>) {
            data = JSONObject.build().put((HashMap<String, ?>) data);
        }
        return netMSG(true, data);
    }

    public JSONObject netMSG(int state, String message, Object data) {
        info.put("errorcode", state).put("record", data);
        if (!StringHelper.isInvalided(message)) {
            info.put("message", message);
        }
        return info;
    }

    public JSONObject netPAGE(int idx, int max, long count, JSONArray record) {
        if (record != null) {
            if (count >= 0) {
                info.put("totalSize", count);
            }
            if (idx >= 0) {
                info.put("currentPage", idx);
            }
            if (max >= 0) {
                info.put("pageSize", max);
            }
        }
        return netMSG(0, record);
    }

    public JSONObject netState(Object state) {
        return netMSG(state, "");
    }
}
