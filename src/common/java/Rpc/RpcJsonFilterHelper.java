package common.java.Rpc;

import org.json.gsc.JSONObject;

import java.util.HashMap;


public class RpcJsonFilterHelper {
    private final JSONObject info;
    private final boolean isUpdate;
    private final HashMap<String, RpcJsonFilterBlock> filterMap;
    private FilterReturn result;

    private RpcJsonFilterHelper(JSONObject info, boolean update) {
        this.info = info;
        this.isUpdate = update;
        this.filterMap = new HashMap<>();
        this.result = FilterReturn.buildTrue();
    }

    public static RpcJsonFilterHelper build(JSONObject info, boolean update) {
        return new RpcJsonFilterHelper(info, update);
    }

    public RpcJsonFilterHelper filter(String key, FilterJsonCallback callback) {
        return filter(key, callback, true, "缺少参数[" + key + "]");
    }

    public RpcJsonFilterHelper filter(String key, boolean required, FilterJsonCallback callback) {
        return filter(key, callback, required, "缺少参数[" + key + "]");
    }

    public RpcJsonFilterHelper filter(String key, FilterJsonCallback callback, boolean required, String nullValueMessage) {
        filterMap.put(key, RpcJsonFilterBlock.build(callback, required, nullValueMessage));
        return this;
    }

    public RpcJsonFilterHelper check() {
        for (String key : filterMap.keySet()) {
            RpcJsonFilterBlock block = filterMap.get(key);
            if (isUpdate) {
                if (info.has(key)) {
                    result = block.getCallback().run(info, key);
                    return this;
                }
                result = FilterReturn.buildTrue();
                return this;
            } else {
                // 不存在的key 同时 是必须的
                if (!info.has(key) && block.isRequired()) {
                    result = FilterReturn.build(false, block.getMessage());
                    return this;
                }
                result = block.getCallback().run(info, key);
                return this;
            }
        }
        return this;
    }

    public FilterReturn getResult() {
        return result;
    }
}

