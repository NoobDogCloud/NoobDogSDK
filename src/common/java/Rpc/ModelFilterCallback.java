package common.java.Rpc;

import org.json.gsc.JSONObject;

// 模型数据输入过滤
@FunctionalInterface
public interface ModelFilterCallback {
    /**
     * @param info   数据模型信息
     * @param update 是否更新
     * @return
     */
    FilterReturn run(JSONObject info, boolean update);
}
