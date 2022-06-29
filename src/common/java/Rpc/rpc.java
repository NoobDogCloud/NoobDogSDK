package common.java.Rpc;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;

public class rpc {
    private String servName;
    private String transferMode;
    private String endpoint;
    private String servPath;
    private HttpContext ctx;
    private boolean needApiAuth;
    private boolean needPublicKey;

    private rpc(String servName) {
        this.needApiAuth = false;
        this.needPublicKey = false;
        setService(servName);
    }

    public static rpc context(HttpContext ctx) {
        return rpc.service(ctx.serviceName())
                .setPath(ctx.className(), ctx.actionName())
                .setContext(ctx);
    }

    // 静态起步方法
    public static rpc service(String servName) {
        return new rpc(servName);
    }

    public rpc setService(String servName) {
        this.servName = servName;
        MicroServiceContext msc = MicroServiceContext.getInstance(this.servName);
        if (msc != null) {
            this.endpoint = msc.bestServer();
            this.transferMode = msc.transfer();
        } else {
            this.transferMode = "http";
            this.endpoint = "";
        }
        return this;
    }

    public rpc setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * 设置自定义http上下文
     */
    public rpc setContext(HttpContext ai) {
        this.ctx = ai;
        return this;
    }

    /**
     * 设置请求path
     */
    public rpc setPath(String className, String actionName) {
        servPath = "/" + className + "/" + actionName;
        return this;
    }

    /**
     * 设置请求path
     */
    public rpc setPath(String rpcURL) {
        servPath = "/" + StringHelper.build(rpcURL).trimFrom('/').toString();
        return this;
    }

    /**
     * 设置授权
     */
    public rpc setApiAuth() {
        needApiAuth = true;
        return this;
    }

    /**
     * 设置请求公钥
     */
    public rpc setApiPublicKey() {
        this.needPublicKey = true;
        return this;
    }

    /**
     * 调用RPC
     */
    public RpcResponse call(Object... args) {
        return switch (transferMode) {
            case MicroServiceContext.TransferKeyName.Pulsar -> RpcPulsar.call(this.toString(), this.ctx, this.needApiAuth, this.needPublicKey, args);
            default -> RpcHttp.call(this.toString(), this.ctx, this.needApiAuth, this.needPublicKey, args);
        };
    }

    public String toPath() {
        return "/" + this.servName + this.servPath;
    }

    /**
     * 获得 WebSocket Rpc请求体
     */
    public RpcWebSocketQuery getWebSocketQueryHeader(Object... args) {
        return RpcWebsocket.query(this.toString("ws"), this.ctx, this.needApiAuth, this.needPublicKey, args);
    }

    /**
     * 获得RPC调用URL
     */
    @Override
    public String toString() {
        return "http://" + endpoint + toPath();
    }

    public String toString(String protocol) {
        return protocol + "://" + endpoint + toPath();
    }
}
