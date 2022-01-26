package common.java.Rpc;

import org.json.gsc.JSONObject;

@FunctionalInterface
public interface FilterJsonCallback {
    FilterReturn run(JSONObject info, String name);
}
