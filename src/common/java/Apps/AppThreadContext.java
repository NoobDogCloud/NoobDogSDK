package common.java.Apps;

import common.java.Http.Server.HttpContext;

public record AppThreadContext(String AppID, String MicroServiceName) {

    public static AppThreadContext build(HttpContext hCtx) {
        return build(hCtx.appId(), hCtx.serviceName());
    }

    public static AppThreadContext build(String AppID, String MicroServiceName) {
        return new AppThreadContext(AppID, MicroServiceName);
    }
}
