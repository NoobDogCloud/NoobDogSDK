package common.java.Apps;

import common.java.Http.Server.HttpContext;

public record AppThreadContext(int AppID, String MicroServiceName) {

    public static AppThreadContext build(HttpContext hCtx) {
        return build(hCtx.appId(), hCtx.serviceName());
    }

    public static AppThreadContext build(int AppID, String MicroServiceName) {
        return new AppThreadContext(AppID, MicroServiceName);
    }
}
