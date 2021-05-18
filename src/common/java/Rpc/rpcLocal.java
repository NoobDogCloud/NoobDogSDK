package common.java.Rpc;

import common.java.Config.Config;
import common.java.HttpServer.HttpContext;

import java.util.Locale;

public class rpcLocal {
    private String className;
    private String actionName;

    private rpcLocal() {
    }

    public static rpcLocal build() {
        return new rpcLocal();
    }

    public rpcLocal setPath(String className, String actionName) {
        this.className = className;
        this.actionName = actionName;
        return this;
    }

    public RpcResponse call(Object... args) {
        var ctx = HttpContext.current();
        String serviceName = ctx.serviceName();
        String baseUrl = "http://127.0.0.1:" + Config.port + "/" + serviceName + "/" + className + "/" + actionName;
        return rpc.call(baseUrl, ctx, false, serviceName.toLowerCase(Locale.ROOT).equals("system"), args);
    }
}
