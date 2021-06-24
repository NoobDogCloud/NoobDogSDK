package common.java.Rpc;

import common.java.Config.Config;
import common.java.Http.Server.Db.HttpContextDb;
import common.java.Http.Server.HttpContext;
import common.java.OAuth.oauthApi;
import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

import java.util.Arrays;

public class RpcWebsocket {
    /**
     * @apiNote 包含参数的URL的使用
     */
    public static RpcWebSocketQuery query(int run_no, String url, HttpContext ctx, boolean api_auth) {
        String[] strArr = url.split("/");
        Object[] args = Arrays.stream(strArr).skip(4).toArray();
        return query(StringHelper.join(strArr, "/", 0, 4), ctx, api_auth, args);
    }

    public static RpcWebSocketQuery query(String path, HttpContext ctx, Object... args) {
        return query(path, ctx, false, args);
    }

    public static RpcWebSocketQuery query(String path, HttpContext ctx, boolean api_auth, Object... args) {
        return query(path, ctx, api_auth, false, args);
    }

    public static RpcWebSocketQuery query(String path, HttpContext ctx, boolean api_auth, boolean public_key, Object... args) {
        String url;
        // 构造http协议rpc完整地址
        if (!path.toLowerCase().startsWith("ws://")) {
            String[] strArr = path.split("/");
            url = rpc.service(strArr[1]).setPath(strArr[2], strArr[3]).toString();
        } else {
            url = path;
        }
        // 是完整协议
        String[] rArr = url.split("//")[1].split("/");
        path = "/" + StringHelper.join(rArr, "/", 1, -1);

        String _path = path + ExecRequest.objects2string(args);

        // 构造httpContent
        if (ctx == null) {
            ctx = HttpContext.current();
            if (ctx == null) {
                ctx = HttpContext.newHttpContext();
            }
        }
        // 设置httpHeader环境
        JSONObject _header = JSONObject.build();
        JSONObject requestHeader = ctx.header();
        for (String key : requestHeader.keySet()) {
            if (!HttpContextDb.DBHeaderKeys.contains(key)) {
                _header.put(key, requestHeader.getString(key));
            }
        }
        // 设置host
        _header.put(HttpContext.GrapeHttpHeader.host, rArr[0]);

        // 设置授权
        if (api_auth) {
            _header.put(HttpContext.GrapeHttpHeader.token, oauthApi.getInstance().getApiToken(rArr[1] + "@" + rArr[2] + "@" + rArr[3]));
        }
        // 设置公钥
        if (public_key) {
            _header.put(HttpContext.GrapeHttpHeader.publicKey, Config.publicKey);
        }

        return RpcWebSocketQuery.build(_path, _header);
    }
}
