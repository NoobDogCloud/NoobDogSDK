package common.java.Rpc;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

@FunctionalInterface
public interface FilterUniqueDataCallback {
    JSONArray<JSONObject> run(JSONObject v);
}
