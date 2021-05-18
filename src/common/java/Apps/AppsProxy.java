package common.java.Apps;

import common.java.HttpServer.HttpContext;
import common.java.MasterProxy.MasterActor;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class AppsProxy {
    private static final String appPath = "apps";
    private static final MasterActor appMapped = MasterActor.getInstance(appPath);
    private static final String servicePath = "services";
    private static final MasterActor serviceMapped = MasterActor.getInstance(servicePath);

    // 获得应用信息
    public static JSONObject getAppInfo(int appId) {
        JSONArray<JSONObject> info = appMapped.getDataByIndex("id", appId);
        if (JSONArray.isInvalided(info)) {
            throw new RuntimeException("当前应用id[" + appId + "]无效!");
        }
        return info.get(0);
    }

    public static JSONObject getAppInfo(String domain) {
        JSONArray<JSONObject> info = appMapped.getDataByIndex("domain", domain);
        if (JSONArray.isInvalided(info)) {
            HttpContext.current().throwOut("当前域名[" + domain + "]未绑定!");
        }
        return info.get(0);
    }

    // 根据AppId 和 微服务名 获得微服务信息
    public static JSONObject getServiceInfo(int appId, String serviceName) {
        // 获得 appId 对应所有微服务信息
        JSONArray<JSONObject> info = serviceMapped.getDataByIndex("appId", appId);
        if (JSONArray.isInvalided(info)) {
            HttpContext.current().throwOut("当前应用[" + appId + "]未部署任何服务!");
        }
        // 找到 name 为 serviceName 的微服务信息
        var serviceInfo = info.mapsByKey("name").getJson(serviceName);
        if (JSONObject.isInvalided(serviceInfo)) {
            HttpContext.current().throwOut("当前服务[" + serviceName + "]未部署在应用[" + appId + "]!");
        }
        return serviceInfo;
    }
}
