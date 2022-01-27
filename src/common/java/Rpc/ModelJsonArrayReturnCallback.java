package common.java.Rpc;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

// 返回的模型数据 JsonArray 过滤器
@FunctionalInterface
public interface ModelJsonArrayReturnCallback {
    JSONArray<JSONObject> run(JSONArray<JSONObject> returnValue);
}
