package common.java.Apps;

import common.java.HttpServer.HttpContext;

public class AppThreadContext {
    public final String MicroServiceName;
    public final int AppID;

    private AppThreadContext(int AppID, String MicroServiceName) {
        this.AppID = AppID;
        this.MicroServiceName = MicroServiceName;
    }

    public static AppThreadContext build(HttpContext hCtx) {
        return build(hCtx.appId(), hCtx.serviceName());
    }

    public static AppThreadContext build(int AppID, String MicroServiceName) {
        return new AppThreadContext(AppID, MicroServiceName);
    }
}
