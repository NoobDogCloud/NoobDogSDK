package common.java.Rpc;

import org.json.gsc.JSONObject;

// 新增或者更新返回值
@FunctionalInterface
public interface ModelInputCallback {
    default Object run(Object returnValue) {
        return run(returnValue, null);
    }

    Object run(Object returnValue, JSONObject input);
}
