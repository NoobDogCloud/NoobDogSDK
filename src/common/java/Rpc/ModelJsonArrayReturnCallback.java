package common.java.Rpc;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

@FunctionalInterface
public interface ModelJsonArrayReturnCallback {
    JSONArray<JSONObject> run(JSONArray<JSONObject> returnValue);
}
