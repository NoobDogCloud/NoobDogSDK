package common.java.Apps;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.Roles.AppRoles;
import common.java.Coordination.Coordination;
import common.java.Http.Common.RequestSession;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.HttpContext;
import io.netty.channel.ChannelId;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 应用上下文
public class AppContext {
    private final HashMap<String, MicroServiceContext> micro_service_context = new HashMap<>();

    private final String appId;
    private final String domain;
    private final String sessionType;
    private final String publishModel;
    private final JSONObject appInfo;
    private final ModelServiceConfig msc;
    private MicroServiceContext microServiceInfo;
    private final AppRoles roles;

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private AppContext(JSONObject appInfo) {
        // 默认使用当前上下文 或者 0
        this.appInfo = appInfo;
        this.appId = this.appInfo.getString("id");
        this.domain = this.appInfo.getString("domain");
        this.sessionType = this.appInfo.getString("session_type");
        this.roles = AppRoles.build(this.appInfo.getJson("roles"));
        this.msc = new ModelServiceConfig(this.appInfo.getJson("config"));
        this.publishModel = this.appInfo.getString("category");
    }

    public static AppContext build(JSONObject appInfo) {
        return new AppContext(appInfo);
    }

    public static AppContext current() {
        return Coordination.getInstance().getAppContextByAppId(HttpContext.current().appId());
    }

    /**
     * 根据指定的appId创建虚拟上下文
     */
    public static AppContext virtualAppContext(String appId, String serviceName) {
        ChannelId cid = RequestSession.buildChannelId();
        RequestSession.create(cid.asLongText()).setWorker();
        AppContext r = Coordination.getInstance().getAppContextByAppId(appId);
        HttpContext h = HttpContext.setNewHttpContext()
                .serviceName(serviceName)
                .appId(appId);
        SocketContext.build(cid.asLongText()).setRequest(h);
        return r;
    }

    /**
     * 将传入socket上下文复制到本线程上下文
     */
    public static void cloneAppContext(SocketContext ctx) {
        ctx.clone();
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
            if (v.getString("appid").equals(appId)) {
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
     * 获得会话维持类型
     */
    public String getSessionType() {
        return this.sessionType;
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
    public String appId() {
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
     * 获得应用的发布模式
     * node-service: 节点服务
     * gateway-service: 网关服务
     * secgateway-service: 安全网关服务
     */
    public String publishModel() {
        return this.publishModel;
    }

    /**
     * 当前上下文启动新线程
     */
    public AppContext thread(Runnable task) {
        // AppThreadContext atc = AppContext.virtualAppContext();
        SocketContext ctx = SocketContext.current();
        pool.execute(() -> {
            // AppContext.virtualAppContext(atc);
            AppContext.cloneAppContext(ctx);
            task.run();
        });
        return this;
    }
}
