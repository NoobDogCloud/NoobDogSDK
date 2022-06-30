package common.java.Rpc;

import org.json.gsc.JSONObject;

import java.util.HashMap;


public class RpcJsonFilterHelper {
    private final JSONObject info;
    private final boolean isUpdate;
    private final HashMap<String, RpcJsonFilterBlockGroup> filterMap;
    private FilterReturn result;

    private RpcJsonFilterHelper(JSONObject info, boolean update) {
        this.info = info;
        this.isUpdate = update;
        this.filterMap = new HashMap<>();
        this.result = FilterReturn.success();
    }

    public static RpcJsonFilterHelper build(JSONObject info, boolean update) {
        return new RpcJsonFilterHelper(info, update);
    }

    public RpcJsonFilterHelper filter(String key, FilterJsonCallback callback) {
        return filter(key, callback, true, "缺少参数[" + key + "]");
    }

    public RpcJsonFilterHelper filter(String key, FilterJsonCallback callback, boolean required) {
        return filter(key, callback, required, "缺少参数[" + key + "]");
    }

    public RpcJsonFilterHelper filter(String key, FilterJsonCallback callback, boolean required, String nullValueMessage) {
        return _filter(key, RpcJsonFilterBlock.build(callback, required, nullValueMessage));
    }

    public RpcJsonFilterHelper _filter(String key, RpcJsonFilterBlock block) {
        filterMap.put(key, filterMap
                .getOrDefault(key, RpcJsonFilterBlockGroup.build())
                .add(block));
        return this;
    }

    public RpcJsonFilterHelper filterUnique(String key, FilterUniqueDataCallback callback) {
        return filterUnique(key, callback, true);
    }

    public RpcJsonFilterHelper filterUnique(String key, FilterUniqueDataCallback callback, boolean required) {
        var fn = RpcJsonFilterBlock.build((json, name) -> {
            // 不包含对应数据不是必须,直接返回成功
            if (!json.containsKey(key) && !required) {
                return FilterReturn.success();
            }
            // 数据不存在，可以使用
            var data = callback.run(json);
            // 是编辑模式
            if (isUpdate) {
                return data.size() > 0 && json.get(name).equals(data.get(0).get(name)) ?
                        FilterReturn.build(false, "[" + name + "]的数据已存在") :
                        FilterReturn.success();
            }
            if (data.size() == 0) {
                return FilterReturn.success();
            }
            return FilterReturn.build(false, "[" + name + "]的数据已被使用");
        }, true, "缺少参数[" + key + "]");
        return _filter(key, fn);
    }

    public RpcJsonFilterHelper check() {
        for (String key : filterMap.keySet()) {
            result = filterMap.get(key).forEach((block) -> {
                FilterReturn r = FilterReturn.success();
                if (isUpdate) {
                    if (info.has(key)) {
                        r = block.getCallback().run(info, key);
                    }
                } else {
                    // 不存在的key 同时 是必须的
                    if (!info.has(key) && block.isRequired()) {
                        return FilterReturn.build(false, block.getMessage());
                    }
                    r = block.getCallback().run(info, key);
                }
                return r;
            });
            // 有一个不通过就返回
            if (!result.isSuccess()) {
                return this;
            }
        }
        return this;
    }

    public FilterReturn getResult() {
        return result;
    }
}

