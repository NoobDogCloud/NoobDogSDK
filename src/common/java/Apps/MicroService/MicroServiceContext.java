package common.java.Apps.MicroService;

import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Apps.MicroService.Model.MicroModel;
import common.java.Apps.MicroService.Model.MicroModelArray;
import common.java.Coordination.Coordination;
import common.java.Http.Server.HttpContext;
import common.java.NetHelper.IPHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

import java.util.*;

public class MicroServiceContext {
    private static final Set<String> TransferKey = new HashSet<>();

    static {
        TransferKey.add(TransferKeyName.Pulsar);
        TransferKey.add(TransferKeyName.Http);
        TransferKey.add(TransferKeyName.Https);
        TransferKey.add(TransferKeyName.RabbitMQ);
        TransferKey.add(TransferKeyName.ActiveMQ);
    }

    private static int currentNo = 0;
    private final JSONObject servInfo;
    private final ModelServiceConfig servConfig;
    private final MicroModelArray servModelInfo;

    private MicroServiceContext(String appId, JSONObject servInfo) {
        // 获得对应微服务信息
        this.servInfo = updateRpcEndpoint(servInfo);
        this.servModelInfo = new MicroModelArray(appId, servInfo.getJson("datamodel"));
        this.servConfig = new ModelServiceConfig(servInfo.getJson("config"));
    }

    // 根据当前服务与目标服务网络状态,更新RPC调用节点
    public static JSONObject updateRpcEndpoint(JSONObject servInfo) {
        String subAddrString = servInfo.getString("clusteraddr");
        String[] cluster_address = subAddrString.split(",");
        List<Long> ip_value_arr = new ArrayList<>();
        for (var address : cluster_address) {
            ip_value_arr.add(IPHelper.ipToLong(address.split(":")[0]) & 0xffff0000);
        }
        try {
            List<String> ip_arr = IPHelper.localIPv4s();
            for (String ip : ip_arr) {
                long local_ip_value = IPHelper.ipToLong(ip) & 0xffff0000;
                if (ip_value_arr.contains(local_ip_value)) {
                    return servInfo.put("subaddr", subAddrString);
                }
            }
        } catch (Exception e) {
            nLogger.errorInfo(e);
        }
        return servInfo;
    }

    public static MicroServiceContext build(String appId, JSONObject serviceInfo) {
        return new MicroServiceContext(appId, serviceInfo);
    }

    public static MicroServiceContext getInstance(String name) {
        HttpContext ctx = HttpContext.current();
        return ctx != null ? Coordination.getInstance().getMicroServiceContext(ctx.appId(), name) : null;
    }

    public static MicroServiceContext current() {
        HttpContext ctx = HttpContext.current();
        return Coordination.getInstance().getMicroServiceContext(ctx.appId(), ctx.serviceName());
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
     * 获得最佳服务节点(服务内部使用内网RPC连接)
     */
    public String bestServer() {
        String[] servers = servInfo.getString("subaddr").split(",");
        // String[] servers = servInfo.getString("clusteraddr").split(",");
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
        // peeraddr 字段需要填充MQ队列连接地址
        String[] servers = this.servInfo.getString("peeraddr").split(",");
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
        public static final String RabbitMQ = "RabbitMQ";
        public static final String ActiveMQ = "ActiveMQ";
        public static final String Http = "http";
        public static final String Https = "https";
    }
}
