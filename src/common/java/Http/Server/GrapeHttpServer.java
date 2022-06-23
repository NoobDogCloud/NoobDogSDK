package common.java.Http.Server;

import common.java.Apps.AppContext;
import common.java.Check.CheckHelper;
import common.java.Coordination.Coordination;
import common.java.Http.Common.RequestSession;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.ApiSubscribe.GscSubscribe;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.json.gsc.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GrapeHttpServer {
    // private final static int bufferLen = 20480;
    private final static ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
    // private final static ExecutorService es = Executors.newCachedThreadPool();

    private static void fixHttpContext(HttpContext ctx) {
        String path = ctx.path();
        String[] blocks = path.split("/");
        String appId = blocks.length > 1 ? blocks[1] : null;
        if (!StringHelper.isInvalided(appId)) {
            // 是特殊请求
            if (appId.equals("system")) {
                ctx.appId("0");
                ctx.path(StringHelper.join(blocks, "/", 1, -1));
            } else {
                // 自动补充appId
                ctx.appId(appId);
                // 自动修正path
                ctx.path(StringHelper.join(blocks, "/", 2, -1));
            }
        }
    }

    /**
     * 过滤恶意请求
     */
    private static boolean filterSafeString(String str) {
        if (str.startsWith("@")) {
            str = str.substring(1);
        }
        return !CheckHelper.IsID(str, 256);
    }

    private static boolean filterSafeQuery(HttpContext ctx) {
        if (filterSafeString(ctx.actionName()) ||
                filterSafeString(ctx.className()) ||
                filterSafeString(ctx.serviceName())
        ) {
            ctx.out(rMsg.netMSG(false, "非法请求"));
            return false;
        }
        return true;
    }

    /**
     * 生成请求上下文，处理HTTP请求信息
     */
    public static void startService(Object _req, ChannelHandlerContext _ctx, JSONObject post) {
        HttpContext ctx = null;
        if (_req == null || _req instanceof JSONObject) {
            //websocket请求
            if (_req != null) {
                ctx = (new HttpContext((JSONObject) _req));
            }
        } else if (_req instanceof HttpRequest) {
            ctx = (new HttpContext((HttpRequest) _req));
        }
        if (ctx != null) {
            ctx.use(_ctx);
            ctx.parameter(post);

            System.out.println(ctx);

            // 是普通http请求
            if (!ctx.isGscRequest()) {
                // 自动修正appId和path
                fixHttpContext(ctx);
            }
            // 恶意请求过滤
            if (!filterSafeQuery(ctx)) {
                return;
            }
            // 正常请求
            _startService(_ctx, ctx);
        }
    }

    public static void _startService(ChannelHandlerContext _ctx, HttpContext ctx) {
        OutResponse oResponse = OutResponse.build(_ctx);
        // 正常线程池
        var future = es.submit(() -> {
            SocketContext sc = RequestSession.get(_ctx.channel().id().asLongText());
            if (sc == null) {
                OutResponse.defaultOut(_ctx, rMsg.netMSG(false, "请求Socket上下文丢失!"));
                return;
            }
            sc.setWorker().setRequest(ctx).setResponse(oResponse);
            try {
                stubLoop(ctx);
            } catch (Exception e) {
                nLogger.errorInfo(e, e.getMessage());
            }
        });
        try {
            future.get();
        } catch (ExecutionException | InterruptedException ex) {
            ex.getCause().printStackTrace();
            oResponse.out(rMsg.netMSG(false, "服务器异常[504]"));
        }
    }

    public static TextWebSocketFrame WebsocketResult(String topic, Object msg) {
        // 补充Websocket结果外衣 返回结果转换成 string
        JSONObject r;
        if (msg == null) {
            r = JSONObject.build();
        } else if (msg instanceof JSONObject) {
            r = (JSONObject) msg;
        } else {
            r = JSONObject.build(msg.toString());
        }
        r.put(HttpContext.GrapeHttpHeader.WebSocket.wsId, topic);
        return new TextWebSocketFrame(r.toString());
    }

    public static void stubLoop(HttpContext ctx) {
        Object rlt = systemCall(ctx);
        SocketContext sCtx = SocketContext.current();
        OutResponse or = sCtx.getResponse();
        if (ctx.method() == HttpContext.Method.websocket) {
            // 响应自动订阅参数(能运行到这里说明请求代码层执行完毕)
            String topic = ctx.getRequestID();
            if (StringHelper.isInvalided(topic)) {
                topic = GscSubscribe.filterSubscribe(sCtx);
            }
            or.out(WebsocketResult(topic, rlt));
        } else {
            or.out(rlt);
        }
    }

    /**
     * 来自netty服务器的调用
     */
    public static Object systemCall(HttpContext ctx) {
        String path = ctx.path();
        String domain = ctx.domain();
        String appId = ctx.appId();
        AppContext appContext;
        Object rsValue = "";

        String[] GrapeRequest = StringHelper.build(path).trimFrom('/').toString().split("/");
        if (GrapeRequest.length >= 2) {
            // 不包含 公钥
            if (StringHelper.isInvalided(ctx.publicKey())) {
                Coordination crd = Coordination.getInstance();
                // appId 无效, 尝试根据域名获得 appId
                if (StringHelper.isInvalided(appId)) {
                    appContext = crd.getAppContext(domain);
                    if (appContext.hasData()) {
                        appId = appContext.appId();
                        ctx.appId(appId);
                    }
                } else {
                    appContext = crd.getAppContextByAppId(appId);
                }
                appContext.service(GrapeRequest[0]);
            }
            // 包含 公钥 服务名必须是 system
            else {
                if (!GrapeRequest[0].equalsIgnoreCase("system")) {
                    HttpContext hCtx = HttpContext.current();
                    if (hCtx != null) {
                        hCtx.throwOut("加密模式->服务名称:" + GrapeRequest[0] + " 无效");
                    }
                    return "";
                }
            }
            // 正式执行请求
            if (GrapeRequest[0].equals("global") || GrapeRequest[1].startsWith("@") || GrapeRequest.length >= 3) {
                rsValue = ExecRequest._run(ctx);
            } else {
                rsValue = rMsg.netMSG(false, "不是合法的GSC请求!");
            }
        }
        return rsValue;
    }
}
