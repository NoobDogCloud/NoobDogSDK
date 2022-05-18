package common.java.Rpc;

import org.json.gsc.JSONObject;

@FunctionalInterface
public interface ModelUpdateCallback {
    Object run(Object returnValue, String[] ids, JSONObject input);
}
