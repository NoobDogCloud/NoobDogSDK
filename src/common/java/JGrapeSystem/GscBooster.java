package common.java.JGrapeSystem;

import common.java.Config.Config;
import common.java.HttpServer.GscServer;
import common.java.MasterProxy.MasterActor;
import common.java.MessageServer.GscPulsarServer;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Locale;

public class GscBooster {


    public static void start() {
        start(Config.serviceName);
    }

    public static void start(String serverName) {
        try {
            // 此时订阅全部用到的数据
            if (!Config.serviceName.toLowerCase(Locale.ROOT).equals("system")) {
                MasterActor.getClient().setConnected(v -> v.subscribe()).subscribe();
            }
            // 设置日志过滤器
            GscBooterBefore._before(serverName);
            // 获得当前服务类型启动方式
            JSONArray<JSONObject> serviceArr = MasterActor.getInstance("services").getData();
            if (JSONArray.isInvalided(serviceArr)) {
                return;
            }
            switch (serviceArr.get(0).getString("transfer")) {
                case "pulsar":
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
