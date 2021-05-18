package common.java.JGrapeSystem;

import common.java.Config.Config;
import common.java.HttpServer.HttpContext;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;

public class GscBooterBefore {
    public static void _before(String serverName) {
        // 设置日志回调
        nLogger.setDebug(Config.debug);
        nLogger.clientFunc = (info, type) -> {
            HttpContext context = HttpContext.current();
            if (context == null) {
                return;
            }
            int appId = context.appId();
            String printInfo = "时间:[" + TimeHelper.build().nowDatetime() + "]-"
                    + "应用:[" + appId + "]-"
                    + "级别:[" + type.toString() + "]-"
                    + "线程:[" + Thread.currentThread().getId() + "]\n"
                    + "信息:\n" + info + "\n"
                    + "============================";
            System.out.println(printInfo);
            /*
            if ( Config.debug ) {
                HttpContext.showMessage(ctx, printInfo);
            }
            */
        };
        // 获得端口
        System.out.println("节点号:[" + Config.nodeID + "]");
        System.out.println("微服务:[" + serverName + "] ...启动完毕");
        System.out.println("监听:" + Config.bindIP + ":" + Config.port + " ...成功");

        if (Config.debug) {
            System.out.println("调试模式:开启");
        }
    }
}
