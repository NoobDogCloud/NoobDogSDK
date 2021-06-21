package common.java.Apps.MicroService.Config;

import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ModelServiceConfig {
    private static final Set<String> cfgKey = new HashSet<>();

    static {
        cfgKey.add(ConfigKeyName.Database);
        cfgKey.add(ConfigKeyName.Cache);
        cfgKey.add(ConfigKeyName.MessageQueue);
        cfgKey.add(ConfigKeyName.Store);
        cfgKey.add(ConfigKeyName.StreamComputer);
        cfgKey.add(ConfigKeyName.BlockComputer);
    }

    private JSONObject store;

    public ModelServiceConfig(JSONObject store) {
        this.store = store;
    }

    public static Set<String> getCfgKey() {
        return cfgKey;
    }

    public static ModelServiceConfig build(JSONObject info) {
        return new ModelServiceConfig(info);
    }

    /**
     * 获得数据库配置
     */
    public String db() {
        return store == null ? null : this.store.getString(ConfigKeyName.Database);
    }

    /**
     * 获得缓存配置
     */
    public String cache() {
        return store == null ? null : this.store.getString(ConfigKeyName.Cache);
    }

    /**
     * 获得队列配置
     */
    public JSONObject mq() {
        return store == null ? null : this.store.getJson(ConfigKeyName.MessageQueue);
    }

    /**
     * 获得存储配置
     */
    public String store() {
        return store == null ? null : this.store.getString(ConfigKeyName.Store);
    }

    /**
     * 获得其他自定义配置
     */
    public String getOtherConfig(String ConfigKey) {
        JSONObject otherInfo = store == null ? null : this.store.getJson(ConfigKeyName.Other);
        return otherInfo == null ? null : otherInfo.getString(ConfigKey);
    }

    /**
     * 填充新的其他配置项
     */
    public ModelServiceConfig otherConfig(JSONObject info) {
        this.store.put(ConfigKeyName.Other, info);
        return this;
    }

    /**
     * 获得other全部配置
     */
    public JSONObject otherConfig() {
        return this.store.getJson(ConfigKeyName.Other);
    }

    /**
     * 返回非other定义项
     */
    public JSONObject getSafeConfig() {
        JSONObject rInfo = JSONObject.build();
        for (String k : cfgKey) {
            if (store.has(k)) {
                rInfo.put(k, store.getString(k));
            }
        }
        return rInfo;
    }

    public ModelServiceConfig putOtherConfig(String key, Object value) {
        if (JSONObject.isInvalided(this.store)) {
            this.store = new JSONObject();
            this.store.put(ConfigKeyName.Other, new JSONObject());
        }
        this.store.getJson(ConfigKeyName.Other).put(key, value);
        return this;
    }

    public ModelServiceConfig removeOtherConfig(String key) {
        if (JSONObject.isInvalided(this.store)) {
            this.store = new JSONObject();
            this.store.put(ConfigKeyName.Other, new JSONObject());
        } else {
            this.store.getJson(ConfigKeyName.Other).remove(key);
        }
        return this;
    }

    public JSONObject toJson() {
        return this.store;
    }

    public static class ConfigKeyName {
        public static final String Database = "db";
        public static final String Cache = "cache";
        public static final String MessageQueue = "mq";
        public static final String Store = "store";
        public static final String Other = "other";
        public static final String StreamComputer = "streamComputer";
        public static final String BlockComputer = "blockComputer";
    }
}

