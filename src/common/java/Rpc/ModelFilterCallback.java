package common.java.Rpc;

import org.json.gsc.JSONObject;

// 模型数据输入过滤
@FunctionalInterface
public interface ModelFilterCallback {
    /**
     * @param info 数据模型信息
     * @param ids  更新是包含的id组
     * @return
     */
    FilterReturn run(JSONObject info, String[] ids);
}
