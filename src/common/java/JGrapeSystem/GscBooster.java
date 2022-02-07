package common.java.JGrapeSystem;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Args.ArgsHelper;
import common.java.Config.Config;
import common.java.Coordination.Coordination;
import common.java.Http.Server.GscServer;
import common.java.MasterProxy.MasterActor;
import common.java.MessageServer.GscPulsarServer;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.rpc;
import common.java.Thread.ThreadHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class GscBooster {

    /**
     * 启动参数启动服务
     *
     * @param args 启动参数
     *             普通服务
     *             -n 服务名称 -h 主控连接信息
     *             主控服务
     *             -p 服务端口 -k 主控密钥
     */
    public static void start(String[] args, Runnable func) {
        // 初始化数据
        var argArr = ArgsHelper.dictionary(args);
        for (String key : argArr.keySet()) {
            switch (key) {
                case "-n" -> Config.serviceName = argArr.get(key).toString();
                case "-h" -> {
                    var host = argArr.get(key).toString();
                    var masterUrl = host.split(":");
                    Config.masterHost = masterUrl[0];
                    Config.masterPort = Integer.parseInt(masterUrl[1]);
                }
                case "-p" -> Config.port = Integer.parseInt(argArr.get(key).toString());
                case "-k" -> {
                    // 主控才有秘钥，所以直接锁死服务名称
                    Config.publicKey = argArr.get(key).toString();
                }
            }
        }
        Config.updateConfig();
        start(func);
    }

    public static void start(String[] args) {
        start(args, null);
    }

    public static void start() {
        start(Config.serviceName);
    }

    public static void start(Runnable func) {
        start(Config.serviceName, func);
    }

    public static void start(String serverName) {
        start(serverName, null);
    }

    /**
     * 启动服务
     *
     * @param serverName 服务名称
     * @param func       服务初始化后，正式启动前的回调函数
     */
    public static void start(String serverName, Runnable func) {
        try {
            JSONArray<JSONObject> serviceArr = null;
            // 此时订阅全部用到的数据
            if (!Config.serviceName.toLowerCase(Locale.ROOT).equals("system")) {
                AtomicBoolean waiting = new AtomicBoolean(true);
                MasterActor.getClient().subscribe(
                        rpc.service("system")
                                .setApiPublicKey()
                                .setEndpoint(Config.masterHost + ":" + Config.masterPort)
                                .setPath("context", "sub")
                                .getWebSocketQueryHeader(Config.serviceName),
                        resp -> {
                            // 初始化订阅数据到全局配置对象
                            Coordination.build(resp.asJson());
                            waiting.set(false);
                        }
                );
                // 等待收到订阅数据
                while (waiting.get()) {
                    ThreadHelper.sleep(100);
                }
                // 获得当前服务类型启动方式
                serviceArr = MasterActor.getInstance("services").getData();
                if (JSONArray.isInvalided(serviceArr)) {
                    return;
                }
            }
            JSONObject currentService = JSONArray.isInvalided(serviceArr) ?
                    JSONObject.build("debug", true)
                            .put("port", 0)
                            .put("transfer", MicroServiceContext.TransferKeyName.Http) :
                    serviceArr.get(0);
            // 根据当前服务调试设置，设置调试模式
            Config.debug = currentService.getBoolean("debug");
            // 根据当前服务端口设置，设置调试模式
            int port = currentService.getInt("port");
            if (port > 0) {
                Config.port = port;
            }
            // 设置日志过滤器
            GscBooterBefore._before(serverName);
            // 载入全部服务类到内存
            ExecRequest.loadServiceApi();
            ExecRequest.loadServiceBefore();
            ExecRequest.loadServiceAfter();
            if (func != null) {
                func.run();
            }
            String transfer = currentService.getString("transfer");
            // 启动http服务
            if (MicroServiceContext.TransferKeyName.Pulsar.equals(transfer)) {
                GscPulsarServer.start(serviceArr);
            } else {
                // ip 从服务配置中获取
                GscServer.start(Config.bindIP, Config.port);
            }
        } catch (Exception e) {
            nLogger.errorInfo(e);
        } finally {
            nLogger.logInfo("服务器关闭");
        }
    }
}
