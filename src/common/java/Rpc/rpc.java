package common.java.Rpc;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.HttpServer.HttpContext;
import common.java.String.StringHelper;

public class rpc {
    private final String servName;
    private final MicroServiceContext msc;
    private String servPath;
    private HttpContext ctx;
    private boolean needApiAuth;
    private boolean needPublicKey;

    private rpc(String servName) {
        this.servName = servName;
        // boolean nullContext = false;
        this.needApiAuth = false;
        msc = new MicroServiceContext(this.servName);
        this.needPublicKey = false;
    }

    // 静态起步方法
    public static rpc service(String servName) {
        return new rpc(servName);
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
        switch (msc.transfer()) {
            case MicroServiceContext.TransferKeyName.Pulsar:
                return RpcPulsar.call(this.toString(), this.ctx, this.needApiAuth, this.needPublicKey, args);
            default:
                return RpcHttp.call(this.toString(), this.ctx, this.needApiAuth, this.needPublicKey, args);
        }
    }

    /**
     * 获得RPC调用URL
     */
    @Override
    public String toString() {
        return "http://" + msc.bestServer() + "/" + this.servName + this.servPath;
    }

    /*
    public void broadCast(Object... args) {
        broadCast(this.servPath, this.ctx, args);
    }
    */
}
