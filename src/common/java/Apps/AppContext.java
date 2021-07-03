package common.java.Apps;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.Roles.AppRoles;
import common.java.Coordination.Coordination;
import common.java.Http.Common.RequestSession;
import common.java.Http.Server.HttpContext;
import io.netty.channel.ChannelId;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;

// 应用上下文
public class AppContext {
    private final HashMap<String, MicroServiceContext> micro_service_context = new HashMap<>();

    private final int appId;
    private final String domain;
    private final JSONObject appInfo;
    private final ModelServiceConfig msc;
    private MicroServiceContext microServiceInfo;
    private final AppRoles roles;

    private AppContext(JSONObject appInfo) {
        // 默认使用当前上下文 或者 0
        this.appInfo = appInfo;
        this.appId = this.appInfo.getInt("id");
        this.domain = this.appInfo.getString("domain");
        this.roles = AppRoles.build(this.appInfo.getJson("roles"));
        this.msc = new ModelServiceConfig(this.appInfo.getJson("config"));
    }

    public static AppContext build(JSONObject appInfo) {
        return new AppContext(appInfo);
    }

    public static AppContext current() {
        return Coordination.getInstance().getAppContext(HttpContext.current().appId());
    }

    /**
     * 根据指定的appId创建虚拟上下文
     */
    public static AppContext virtualAppContext(int appId, String serviceName) {
        ChannelId cid = RequestSession.buildChannelId();
        RequestSession.create(cid.asLongText()).setWorker();
        AppContext r = Coordination.getInstance().getAppContext(appId);
        HttpContext.setNewHttpContext()
                .serviceName(serviceName)
                .appId(appId);
        return r;
    }

    public MicroServiceContext service(String name) {
        this.microServiceInfo = micro_service_context.get(name);
        return this.microServiceInfo;
    }

    /**
     * 获得当前应用上下文
     */
    public static AppThreadContext virtualAppContext() {
        return AppThreadContext.build(HttpContext.current());
    }

    /**
     * 设置当前线程上下文
     */
    public static AppContext virtualAppContext(AppThreadContext atc) {
        return virtualAppContext(atc.AppID(), atc.MicroServiceName());
    }

    public AppContext loadPreMicroContext(JSONArray<JSONObject> service) {
        // 找到当前 appId 对应数据
        for (JSONObject v : service) {
            if (v.getInt("appid") == appId) {
                micro_service_context.put(v.getString("name"), MicroServiceContext.build(appId, v));
            }
        }
        return this;
    }

    public boolean hasData() {
        return this.appInfo != null;
    }

    /**
     * 获得应用名称
     */
    public String name() {
        return this.appInfo.getString("name");
    }

    /**
     * 获得应用的域名
     */
    public String domain() {
        return this.domain;
    }

    /**
     * 获得应用的配置
     */
    public ModelServiceConfig config() {
        return this.msc;
    }

    /**
     * 获得当前应用id
     */
    public int appId() {
        return this.appId;
    }

    /**
     * 获得当前应用角色定义
     */
    public AppRoles roles() {
        return roles;
    }

    /**
     * 获得应用包含的微服务的信息
     */
    public MicroServiceContext microServiceInfo() {
        return this.microServiceInfo;
    }

    /**
     * 当前上下文启动新线程
     */
    public AppContext thread(Runnable task) {
        AppThreadContext atc = AppContext.virtualAppContext();
        Thread.ofVirtual().start(() -> {
            AppContext.virtualAppContext(atc);
            task.run();
        });
        return this;
    }
}
