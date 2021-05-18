package common.java.Coordination.Client.Store;

import org.json.gsc.JSONObject;

import java.util.HashMap;

public class App {
    private final JSONObject store;               // 当前应用对应配置信息
    // 服务名, 服务上下文
    private final HashMap<String, JSONObject> services = new HashMap<>();     // 当前应用绑定本微服务的配置信息

    private App(JSONObject data) {
        store = data;
    }

    public static App build(JSONObject data) {
        return new App(data);
    }

    public JSONObject getStore() {
        return store;
    }

    // 设置到微服务信息到当前应用
    public App putService(JSONObject servInfo) {
        if (!servInfo.containsKey("name")) {
            throw new RuntimeException("微服务信息错误->名称字段不存在!");
        }
        services.put(servInfo.getString("name"), servInfo);
        return this;
    }

    // 获得当前应用对应微服务配置信息
    public HashMap<String, JSONObject> getServiceInfo() {
        return this.services;
    }

    public JSONObject getServiceInfo(String serviceName) {
        return this.services.get(serviceName);
    }

    public boolean check(String key, Object val) {
        return store.check(key, val);
    }

}
