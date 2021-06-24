package common.java.GscCommon;

import common.java.Session.UserSession;
import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

public class JsonValueFilter {
    private final JSONObject in;
    private final JSONObject otherData;
    private JSONObject userSession;

    public JsonValueFilter(JSONObject in) {
        this.in = in;
        this.otherData = new JSONObject();
        this.userSession = UserSession.current().getDatas();
    }

    public static JsonValueFilter build(JSONObject in) {
        return new JsonValueFilter(in);
    }

    public JsonValueFilter addOther(JSONObject in) {
        if (in != null) {
            otherData.putAll(in);
        }
        return this;
    }

    public JsonValueFilter setSessionInfo(JSONObject sessionInfo) {
        this.userSession = sessionInfo;
        return this;
    }

    public final JSONObject filter() {
        JSONObject rJson = new JSONObject();
        for (String key : in.keySet()) {
            rJson.put(key, this.conv(in.get(key)));
        }
        return rJson;
    }

    private Object conv(Object val) {
        Object r = val;
        String[] cap = StringHelper.toString(val).split("::");
        if (cap.length > 1) {
            String caption = cap[0].toLowerCase();
            switch (caption) {
                case "session" -> r = JSONObject.isInvalided(this.userSession) ? null : this.userSession.get(cap[1]);
                case "other" -> r = this.otherData.getString(cap[1]);
            }
        }
        return r;
    }
}
