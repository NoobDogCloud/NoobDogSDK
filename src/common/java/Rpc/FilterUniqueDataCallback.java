package common.java.Rpc;

import org.json.gsc.JSONObject;

// 模式输入数据单个字段唯一性过滤
@FunctionalInterface
public interface FilterUniqueDataCallback {
    /**
     * @param v 需要过滤的数据(item)
     * @return
     */
    boolean run(JSONObject v);
}
