package common.java.JGrapeSystem;

import common.java.Config.Config;
import common.java.Http.Server.HttpContext;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;

public class GscBoosterBefore {
    public static void _before(String serverName) {
        // 设置日志回调
        nLogger.setDebug(Config.debug);
        nLogger.clientFunc = (info, type) -> {
            HttpContext context = HttpContext.current();
            if (context == null) {
                return;
            }
            String appId = context.appId();
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
        nLogger.logInfo("节点号:[" + Config.getNodeID() + "]");
        nLogger.logInfo("微服务:[" + serverName + "] ...启动完毕");
        nLogger.logInfo("监听:" + Config.bindIP + ":" + Config.port + " ...成功");

        if (Config.debug) {
            nLogger.logInfo("调试模式:开启");
        }
    }
}
