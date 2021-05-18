package common.java.Rpc;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class RpcResponse {
    private final JSONObject r;

    public RpcResponse(String rString) {
        this.r = rString == null ? new JSONObject() : JSONObject.toJSON(rString);
    }

    public RpcResponse(JSONObject rJson) {
        this.r = rJson;
    }

    public static RpcResponse build(String rString) {
        return new RpcResponse(rString);
    }

    public static RpcResponse build(JSONObject rJson) {
        return new RpcResponse(rJson);
    }

    @Override
    public String toString() {
        return this.r.toString();
    }

    public int errCode() {
        return r != null && r.containsKey("errorcode") ? r.getInt("errorcode") : -1;
    }

    public boolean status() {
        return errCode() == 0;
    }

    public boolean hasMessage() {
        return r != null && r.containsKey("message");
    }

    public String message() {
        return r == null ? "" : r.getString("message");
    }

    public String asString() {
        return r == null ? "" : r.getString("record");
    }

    public RpcResponsePage asPage() {
        return new RpcResponsePage(r == null ? null : r.getJson("record"));
    }

    public JSONObject asJson() {
        return r == null ? null : r.getJson("record");
    }

    public int asInt() {
        return r == null ? null : r.getInt("record");
    }

    public long asLong() {
        return r == null ? null : r.getLong("record");
    }

    public float asFloat() {
        return r == null ? null : r.getFloat("record");
    }

    public double asDouble() {
        return r == null ? null : r.getDouble("record");
    }

    public boolean asBoolean() {
        return r != null && r.getBoolean("record");
    }

    public JSONArray<JSONObject> asJsonArray() {
        if (r == null) {
            return null;
        }
        JSONArray<JSONObject> t0 = r.getJsonArray("record");
        if (t0 == null) {
            JSONObject t1 = r.getJson("record");
            if (!JSONObject.isInvalided(t1)) {
                t0 = JSONArray.build(t1);
            }
        }
        return t0;
    }

    public static class RpcResponsePage {
        private final JSONObject r;

        public RpcResponsePage(JSONObject data) {
            this.r = data;
        }

        public int total() {
            return r == null ? 0 : r.getInt("totalSize");
        }

        public int idx() {
            return r == null ? 0 : r.getInt("idx");
        }

        public int max() {
            return r == null ? 0 : r.getInt("max");
        }

        public JSONArray record() {
            return r == null ? null : r.getJsonArray("data");
        }
    }
}
