package common.java.JGrapeSystem;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Config.Config;
import common.java.Coordination.Coordination;
import common.java.Http.Server.GscServer;
import common.java.MasterProxy.MasterActor;
import common.java.MessageServer.GscPulsarServer;
import common.java.Rpc.rpc;
import common.java.Thread.ThreadHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class GscBooster {


    public static void start() {
        start(Config.serviceName);
    }

    public static void start(Runnable func) {
        start(Config.serviceName, func);
    }

    public static void start(String serverName) {
        start(serverName, null);
    }

    public static void start(String serverName, Runnable func) {
        try {
            JSONArray<JSONObject> serviceArr = null;
            // 此时订阅全部用到的数据
            if (!Config.serviceName.toLowerCase(Locale.ROOT).equals("system")) {
                // MasterActor.getClient().setConnected(v -> v.subscribe()).subscribe();
                AtomicBoolean waiting = new AtomicBoolean(true);
                MasterActor.getClient().subscribe(
                        rpc.service("system").setPath("context", "sub").getWebSocketQueryHeader(Config.serviceName),
                        resp -> {
                            // 初始化订阅数据到全局配置对象
                            Coordination.build(resp.asJson());
                            waiting.set(false);
                        }
                );
                // 等待收到订阅数据
                while (waiting.get() == true) {
                    ThreadHelper.sleep(100);
                }
                // 获得当前服务类型启动方式
                serviceArr = MasterActor.getInstance("services").getData();
                if (JSONArray.isInvalided(serviceArr)) {
                    return;
                }
            }
            // 设置日志过滤器
            GscBooterBefore._before(serverName);

            if (func != null) {
                func.run();
            }
            String transfer = JSONArray.isInvalided(serviceArr) ? "default" : serviceArr.get(0).getString("transfer");
            switch (transfer) {
                case MicroServiceContext.TransferKeyName.Pulsar:
                    GscPulsarServer.start(serviceArr);
                    break;
                default:
                    // 启动http服务
                    GscServer.start(Config.bindIP, Config.port);
            }
        } catch (Exception e) {
            nLogger.errorInfo(e);
        } finally {
            nLogger.logInfo("服务器关闭");
        }
    }
}
