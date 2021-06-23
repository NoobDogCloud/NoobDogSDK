package common.java.Apps.MicroService;

import common.java.Apps.AppsProxy;
import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.Model.MicroModel;
import common.java.Apps.MicroService.Model.MicroModelArray;
import common.java.Http.Server.HttpContext;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MicroServiceContext {
    private static final Set<String> TransferKey = new HashSet<>();

    static {
        TransferKey.add(TransferKeyName.Pulsar);
        TransferKey.add(TransferKeyName.Http);
    }

    private static int currentNo = 0;
    private JSONObject servInfo;
    private ModelServiceConfig servConfig;
    private MicroModelArray servModelInfo;

    private MicroServiceContext() {
        // 获得当前微服务名
        HttpContext ctx = HttpContext.current();
        init(ctx.appId(), ctx.serviceName());
    }

    public MicroServiceContext(int appId, String servName) {
        init(appId, servName);
    }

    public MicroServiceContext(String servName) {
        init(HttpContext.current().appId(), servName);
    }

    public static MicroServiceContext current() {
        return new MicroServiceContext();
    }

    private void init(int appId, String servName) {
        // 获得对应微服务信息
        this.servInfo = AppsProxy.getServiceInfo(appId, servName);
        if (this.servInfo != null) {
            this.servModelInfo = new MicroModelArray(this.servInfo.getJson("dataModel"));
            this.servConfig = new ModelServiceConfig(this.servInfo.getJson("config"));
        }
    }

    public static Set<String> TransferKey() {
        return TransferKey;
    }

    /**
     * 判断是否是有效对象
     */
    public boolean hasData() {
        return this.servModelInfo != null;
    }

    /**
     * 获得最佳服务节点
     */
    public String bestServer() {
        String[] servers = servInfo.getString("peerAddr").split(",");
        currentNo++;
        return servers[currentNo % servers.length];
    }

    /**
     * 获得服务通讯协议
     */
    public String transfer() {
        return this.servInfo.getString("transfer");
    }

    /**
     * 获得订阅服务通讯协议
     */
    public String bestSubscribe() {
        String[] servers = this.servInfo.getString("subAddr").split(",");
        currentNo++;
        return servers[currentNo % servers.length];
    }

    /**
     * 获得微服务的配置
     */
    public ModelServiceConfig config() {
        return this.servConfig;
    }

    /**
     * 获得微服务的业务模型
     */
    public MicroModel model(String modelName) {
        return this.servModelInfo.microModel(modelName);
    }

    /**
     * 获得全部微服务的业务模型
     */
    public HashMap<String, MicroModel> model() {
        return this.servModelInfo.microModel();
    }

    /**
     * 是否处于调试状态的服务
     */
    public boolean isDebug() {
        return this.servInfo.getBoolean("debug");
    }

    public static class TransferKeyName {
        public static final String Pulsar = "pulsar";
        public static final String Http = "http";
    }
}
