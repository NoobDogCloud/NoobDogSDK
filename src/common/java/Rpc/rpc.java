package common.java.Rpc;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Config.Config;
import common.java.HttpServer.HttpContext;
import common.java.HttpServer.SpecHeader.Db.HttpContextDb;
import common.java.OAuth.oauthApi;
import common.java.String.StringHelper;
import common.java.Thread.ThreadHelper;
import common.java.nLogger.nLogger;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.RequestBodyEntity;
import kong.unirest.Unirest;
import org.json.gsc.JSONObject;

import java.util.Arrays;

public class rpc {
    private static final int max_retry = 30;           // 重试次数30
    private static final int delay_retry = 5000;       // 重试间隔5s
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

    public static RpcResponse call(String path, HttpContext ctx, Object... args) {
        return call(path, ctx, false, args);
    }

    public static RpcResponse call(String path, HttpContext ctx, boolean api_auth, Object... args) {
        return call(path, ctx, api_auth, false, args);
    }

    public static RpcResponse call(String path, HttpContext ctx, boolean api_auth, boolean public_key, Object... args) {
        String url = path;
        // 构造http协议rpc完整地址
        if (!path.toLowerCase().startsWith("http://")) {
            String[] strArr = path.split("/");
            url = rpc.service(strArr[1]).setPath(strArr[2], strArr[3]).toString();
        } else {
            path = path.split("//")[1];
        }
        String[] rArr = path.split("/");
        // 设置请求参数[get]
        // url += (( args != null ) ? ExecRequest.objects2string(args) : "");
        // 创建http对象[get]
        // GetRequest r = Unirest.get( url );
        // 创建http对象[post]
        HttpRequestWithBody r = Unirest.post(url);

        // 构造httpContent
        if (ctx == null) {
            ctx = HttpContext.current();
            if (ctx == null) {
                ctx = HttpContext.newHttpContext();
            }
        }
        // 设置httpHeader环境
        JSONObject requestHeader = ctx.header();
        for (String key : requestHeader.keySet()) {
            if (!HttpContextDb.DBHeaderKeys.contains(key)) {
                r.header(key, requestHeader.getString(key));
            }
        }
        // 设置授权
        if (api_auth) {
            r.header(HttpContext.GrapeHttpHeader.token, oauthApi.getInstance().getApiToken(rArr[1] + "@" + rArr[2] + "@" + rArr[3]));
        }
        // 设置公钥
        if (public_key) {
            r.header(HttpContext.GrapeHttpHeader.publicKey, Config.publicKey);
        }
        // 设置请求参数[post]
        RequestBodyEntity rBody = r.body(args != null ? ExecRequest.objects2poststring(args) : "");
        String rs = null;
        for (int err_i = 0; err_i < max_retry; err_i++) {
            try {
                rs = rBody.asString().getBody();
                break;
            } catch (Exception e) {
                if (err_i >= max_retry) {
                    nLogger.debugInfo(e, "服务:[" + path + "] ->连接失败！");
                    rs = null;
                } else {
                    ThreadHelper.sleep(delay_retry);
                    continue;   // 无意义
                }
            }
        }
        return RpcResponse.build(rs);
    }

    /**
     * @apiNote 包含参数的URL的使用
     */
    public static RpcResponse call(int run_no, String url, HttpContext ctx, boolean api_auth) {
        String[] strArr = url.split("/");
        Object[] args = Arrays.stream(strArr).skip(4).toArray();
        return call(StringHelper.join(strArr, "/", 0, 4), ctx, api_auth, args);
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
        return call(this.toString(), this.ctx, this.needApiAuth, this.needPublicKey, args);
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
