package common.java.Rpc;

import org.json.gsc.JSONObject;

@FunctionalInterface
public interface ModelJsonReturnCallback {
    JSONObject run(JSONObject returnValue);
}
