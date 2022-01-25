package common.java.Rpc;

import org.json.gsc.JSONObject;

@FunctionalInterface
public interface ModelFilterCallback {
    FilterReturn run(JSONObject info, boolean update);
}
