package common.java.Apps;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.Roles.AppRoles;
import common.java.Http.Common.RequestSession;
import common.java.Http.Server.HttpContext;
import io.netty.channel.ChannelId;
import org.json.gsc.JSONObject;

// 应用上下文
public class AppContext {
    public static final String SessionKey = "AppContext";
    // private static final ExecutorService globalService = Executors.newCachedThreadPool();
    private int appId;
    private String domain;
    private JSONObject appInfo;
    private ModelServiceConfig msc;
    private MicroServiceContext microServiceInfo;
    private AppRoles roles;

    private AppContext() {
        // 默认使用当前上下文 或者 0
        init(HttpContext.current().appId());
    }

    private AppContext(int appId) {
        init(appId);
    }

    private AppContext(String domain) {
        init(domain);
    }

    public static AppContext build(int appId) {
        return build(appId, HttpContext.current().serviceName());
    }

    public static AppContext build(int appId, String serviceName) {
        return (new AppContext(appId)).loadMircServiceInfo(serviceName);
    }

    public static AppContext build(String domain) {
        return build(domain, HttpContext.current().serviceName());
    }

    public static AppContext build(String domain, String serviceName) {
        return (new AppContext(domain)).loadMircServiceInfo(serviceName);
    }

    public static AppContext current() {
        AppContext r = RequestSession.getValue(AppContext.SessionKey);
        if (r == null) {
            r = new AppContext();
            RequestSession.setValue(AppContext.SessionKey, r);
            String serviceName = HttpContext.current().serviceName();
            r.loadMircServiceInfo(serviceName);
        }
        return r;
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
        return virtualAppContext(atc.AppID, atc.MicroServiceName);
    }

    /**
     * 根据指定的appId创建虚拟上下文
     */
    public static AppContext virtualAppContext(int appId, String serviceName) {
        ChannelId cid = RequestSession.buildChannelId();
        RequestSession.setChannelID(cid);
        RequestSession.create(cid);
        AppContext r = new AppContext(appId);
        RequestSession.setValue(AppContext.SessionKey, r);
        HttpContext.setNewHttpContext()
                .serviceName(serviceName)
                .appId(appId);
        return r;
    }

    private void init(int appId) {
        init(AppsProxy.getAppInfo(appId));
    }

    private void init(String domain) {
        init(AppsProxy.getAppInfo(domain));
    }

    private void init(JSONObject appInfo) {
        this.appInfo = appInfo;
        if (this.appInfo != null) {
            this.appId = this.appInfo.getInt("id");
            this.domain = this.appInfo.getString("domain");
            this.roles = AppRoles.build(this.appInfo.getJson("roles"));
            this.msc = new ModelServiceConfig(this.appInfo.getJson("config"));
        }

    }

    // 获得
    private AppContext loadMircServiceInfo(String serviceName) {
        // 单服务只管自己的微服务信息
        MicroServiceContext msc = new MicroServiceContext(this.appId, serviceName);
        if (msc.hasData()) {
            this.microServiceInfo = msc;
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
