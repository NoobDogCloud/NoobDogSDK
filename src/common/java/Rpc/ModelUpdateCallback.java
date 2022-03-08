package common.java.Rpc;

import org.json.gsc.JSONObject;

public interface ModelUpdateCallback {
    default Object run(Object returnValue) {
        return run(returnValue, null, null);
    }

    default Object run(Object returnValue, String[] ids) {
        return run(returnValue, ids, null);
    }

    Object run(Object returnValue, String[] ids, JSONObject input);
}
