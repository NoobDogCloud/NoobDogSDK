package common.java.Rpc;

import org.json.gsc.JSONObject;

@FunctionalInterface
public interface FilterJsonCallback {
    /**
     * @param info 单条JSON数据（item）
     * @param name 字段名
     * @return
     */
    FilterReturn run(JSONObject info, String name);
}
