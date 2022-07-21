package common.java.Coordination;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Config.Config;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Coordination {
    private static Coordination handle;
    private final AtomicReference<JSONArray<JSONObject>> apps = new AtomicReference<>();
    private final AtomicReference<JSONArray<JSONObject>> services = new AtomicReference<>();
    private final AtomicReference<JSONArray<JSONObject>> configs = new AtomicReference<>();

    private final AtomicReference<JSONObject> proxy_services = new AtomicReference<>();

    private final ConcurrentHashMap<String, AppContext> app_context = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> domain_context = new ConcurrentHashMap<>();

    private Coordination() {
    }

    public static Coordination build(JSONObject data) {
        // 必须先运行 getInstance 写入实例句柄，才可以载入（载入会用到 Coordination 自身）
        getInstance().init(data);
        return handle;
    }

    public static Coordination getInstance() {
        if (handle == null) {
            handle = new Coordination();
        }
        return handle;
    }

    private void init(JSONObject data) {
        try {
            // 记录全局数据
            apps.set(data.getJsonArray("apps"));
            services.set(data.getJsonArray("services"));
            configs.set(data.getJsonArray("configs"));
            proxy_services.set(data.getJson("proxy_services"));
            // 生成上下文
            JSONArray<JSONObject> appArr = apps.get();
            JSONArray<JSONObject> svcArr = services.get();
            for (JSONObject v : appArr) {
                String appId = v.getString("id");
                AppContext aCtx = AppContext.build(v);
                app_context.put(appId, aCtx);
                aCtx.loadPreMicroContext(svcArr);
                String domain = v.getString("domain");
                if (!StringHelper.isInvalided(domain)) {
                    domain_context.put(domain, appId);
                }
            }
        } catch (Exception e) {
            nLogger.errorInfo(e);
            System.out.println("初始化平台数据失败!");
        }
    }

    public AppContext getAppContextByAppId(String appId) {
        AppContext ctx = app_context.get(appId);
        if (ctx == null) {
            var hCtx = HttpContext.current();
            if (hCtx != null) {
                hCtx.throwOut("当前应用id[" + appId + "]无效!");
            }
        }
        return ctx;
    }

    public AppContext getAppContext(String domain) {
        String appId = domain_context.get(domain);
        if (appId == null) {
            var hCtx = HttpContext.current();
            if (hCtx != null) {
                hCtx.throwOut("服务:" + Config.serviceName + "->当前域名[" + domain + "]未绑定!");
            }
        }
        return getAppContextByAppId(appId);
    }

    public MicroServiceContext getMicroServiceContext(String appId, String serviceName) {
        // 必须先实例化 AppContext 再设置 ServiceContext
        AppContext app_ctx = getAppContextByAppId(appId);
        MicroServiceContext msc_ctx = app_ctx.service(serviceName);
        if (msc_ctx == null) {
            var hCtx = HttpContext.current();
            if (hCtx != null) {
                hCtx.throwOut("当前服务[" + serviceName + "]未部署在应用[" + appId + "]!");
            }
        }
        return msc_ctx;
    }

    public JSONArray<JSONObject> getAppArray() {
        return apps.get();
    }

    public JSONArray<JSONObject> getServiceArray() {
        return services.get();
    }

    public JSONArray<JSONObject> getConfigArray() {
        return configs.get();
    }

    public JSONObject getProxyServiceArray() {
        return proxy_services.get();
    }

    public JSONArray<String> getServiceProxy(String serviceName) {
        return proxy_services.get().getJsonArray(serviceName);
    }

    public JSONArray<JSONObject> getData(String className) {
        return switch (className) {
            case "apps" -> getAppArray();
            case "services" -> getServiceArray();
            case "configs" -> getConfigArray();
            default -> throw new IllegalStateException("Unexpected value: " + className);
        };
    }
}
