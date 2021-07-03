package common.java.Http.Server;

import common.java.Apps.AppContext;
import common.java.Config.Config;
import common.java.Coordination.Coordination;
import common.java.Http.Common.RequestSession;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.Subscribe.SubscribeGsc;
import common.java.Number.NumberHelper;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.json.gsc.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GrapeHttpServer {

    private final static int bufferLen = 20480;
    private final static ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
    // private final static ExecutorService es = Executors.newCachedThreadPool();

    private static void fixHttpContext(HttpContext ctx) {
        String path = ctx.path();
        String[] blocks = path.split("/");
        int appId = blocks.length > 1 ? NumberHelper.number2int(blocks[1]) : 0;
        if (appId > 0) {
            // 自动补充appId
            ctx.appId(appId);
            // 自动修正path
            ctx.path(StringHelper.join(blocks, "/", 2, -1));
        }
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
            // 是普通http请求
            if (!ctx.isGscRequest()) {
                // 自动修正appId和path
                fixHttpContext(ctx);
            }
            // 正常请求
            _startService(_ctx, ctx);
        }
    }

    public static void _startService(ChannelHandlerContext _ctx, HttpContext ctx) {
        // 正常线程池
        es.submit(() -> {
            // 为正常线程创建协程
            ThreadLocal<ExecutorService> child_thread_es = new ThreadLocal<>();
            ExecutorService child_es = child_thread_es.get();
            if (child_es == null) {
                child_es = Executors.newVirtualThreadExecutor();
                child_thread_es.set(child_es);
            }
            child_es.submit(() -> {
                OutResponse oResponse = OutResponse.build(_ctx);
                SocketContext sc = RequestSession.get(_ctx.channel().id().asLongText());
                if (sc == null) {
                    OutResponse.defaultOut(_ctx, rMsg.netMSG(false, "请求Socket上下文丢失!"));
                    return;
                }
                sc.setWorker().setRequest(ctx).setResponse(oResponse);
                try {
                    stubLoop(ctx);
                } catch (Exception e) {
                    if (Config.debug) {
                        oResponse.out(rMsg.netMSG(false, e.getMessage()));
                    }
                }
            });
        });
    }

    public static void stubLoop(HttpContext ctx) {
        Object rlt = systemCall(ctx);
        OutResponse or = SocketContext.current().getResponse();
        if (ctx.method() == HttpContext.Method.websocket) {
            // 响应自动订阅参数(能运行到这里说明请求代码层执行完毕)
            String topic = SubscribeGsc.filterSubscribe(ctx);
            // 补充Websocket结果外衣 返回结果转换成 string
            JSONObject r = rlt == null ? JSONObject.build() : JSONObject.build(rlt.toString());
            r.put(HttpContext.GrapeHttpHeader.WebSocket.wsId, topic);
            or.out(new TextWebSocketFrame(r.toString()));
        } else {
            or.out(rlt);
        }
    }

    /**
     * 来自netty服务器的调用
     */
    public static Object systemCall(HttpContext ctx) {
        String path = ctx.path();
        String host = ctx.host();
        int appId = ctx.appId();
        AppContext appContext;
        Object rsValue = "";

        String[] GrapeRequest = StringHelper.build(path).trimFrom('/').toString().split("/");
        if (GrapeRequest.length >= 2) {
            // 不包含 公钥
            if (StringHelper.isInvalided(ctx.publicKey())) {
                Coordination crd = Coordination.getInstance();
                // appId 无效, 尝试根据域名获得 appId
                if (appId == 0) {
                    appContext = crd.getAppContext(host);
                    if (appContext.hasData()) {
                        appId = appContext.appId();
                        ctx.appId(appId);
                    }
                } else {
                    appContext = crd.getAppContext(appId);
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
            if (!ctx.invaildGscResquest()) {
                rsValue = ExecRequest._run(ctx);
            } else {
                rsValue = rMsg.netMSG(false, "不是合法的GSC请求!");
            }
        }
        return rsValue;
    }
}
