package common.java.HttpServer;

import common.java.Apps.AppContext;
import common.java.Config.Config;
import common.java.Http.Mime;
import common.java.HttpServer.Common.RequestSession;
import common.java.HttpServer.SpecHeader.Db.HttpContextDb;
import common.java.Number.NumberHelper;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.RpcLocation;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.stream.ChunkedStream;
import org.json.gsc.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GrapeHttpServer {

    private final static int bufferLen = 20480;
    private final static ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
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
            HttpContext ctxFinal = ctx;
            // 正常线程池
            es.submit(() -> {
                // 为正常线程创建协程
                ThreadLocal<ExecutorService> child_thread_es = new ThreadLocal();
                ExecutorService child_es = child_thread_es.get();
                if (child_es == null) {
                    child_es = Executors.newVirtualThreadExecutor();
                    child_thread_es.set(child_es);
                }
                child_es.submit(() -> {
                    RequestSession.setChannelID(_ctx.channel().id());
                    try {
                        stubLoop(ctxFinal);
                    } catch (Exception e) {
                        if (Config.debug) {
                            writeHttpResponse(_ctx, rMsg.netMSG(false, e.getMessage()));
                        }
                    }
                });
            });
        }
    }

    public static void stubLoop(HttpContext ctx) {
        Object rlt = GrapeHttpServer.EventLoop(ctx);
        if (ctx.method() == HttpContext.Method.websocket) {
            rlt = new TextWebSocketFrame(rlt.toString());
        }

        GrapeHttpServer.writeHttpResponse(ctx.channelContext(), rlt);
    }

    public static Object EventLoop(HttpContext ctx) {
        RequestSession.setValue(HttpContext.SessionKey, ctx);
        RequestSession.setValue(HttpContext.ResponseSessionKey, new DefaultFullHttpResponse(HTTP_1_1, OK));
        RequestSession.setValue(HttpContext.RequestSessionKey, ctx.getRequest());
        return systemCall(ctx);
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
                // appId 无效, 尝试根据域名获得 appId
                if (appId == 0) {
                    appContext = AppContext.build(host, GrapeRequest[0]);
                    if (appContext.hasData()) {
                        appId = appContext.appId();
                        ctx.appId(appId);
                    }
                } else {
                    appContext = AppContext.build(appId, GrapeRequest[0]);
                    // 微服务名无效或者应用ID无效
                    if (!appContext.hasData()) {
                        HttpContext.current().throwOut("[应用ID:" + appId + " 无效] 或者 [微服务名称:" + GrapeRequest[0] + " 无效]");
                        return "";
                    }
                }
            }
            // 包含 公钥 服务名必须是 system
            else {
                if (!GrapeRequest[0].equalsIgnoreCase("system")) {
                    HttpContext.current().throwOut("加密模式->服务名称:" + GrapeRequest[0] + " 无效");
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

    public static void ZeroResponse(ChannelHandlerContext ctx) {
        writeHttpResponse(ctx, "");
    }

    public static void location(ChannelHandlerContext ctx, String newURL) {
        GrapeHttpServer.writeHttpResponse(ctx, "".getBytes(), new JSONObject("Location", newURL));
    }

    public static void location(ChannelHandlerContext ctx, String newURL, JSONObject exHeader) {
        JSONObject header = new JSONObject("Location", newURL);
        if (exHeader != null) {
            header.putAll(exHeader);
        }
        GrapeHttpServer.writeHttpResponse(ctx, "".getBytes(), header);
    }

    private static void addHeader(HttpResponse response, boolean hasChunked) {
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Access-Control-Allow-Headers",
                HttpContext.GrapeHttpHeader.sid + " ," +
                        HttpContext.GrapeHttpHeader.token + " ," +
                        HttpContext.GrapeHttpHeader.appId + " ," +
                        HttpContext.GrapeHttpHeader.publicKey + " ," +
                        HttpContextDb.fields + " ," +
                        HttpContextDb.sorts + " ," +
                        HttpContextDb.options


        );
        if (hasChunked) {
            response.headers().set("Transfer-Encoding", "chunked");
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, InputStream responseData, JSONObject exHeader) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        addHeader(response, true);
        if (exHeader != null) {
            for (String key : exHeader.keySet()) {
                response.headers().set(key, exHeader.getString(key));
            }
        }

        if (responseData != null) {
            ChannelFuture sendByteFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(responseData, bufferLen)), ctx.newProgressivePromise());
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            sendByteFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    try {
                        responseData.close();
                    } catch (Exception e) {
                        nLogger.logInfo("steam is closed");
                    }
                }
            });
        } else {
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, byte[] responseData, JSONObject exHeader) {
        // FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        FullHttpResponse response = HttpContext.response();
        if (response == null) {
            response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        }
        addHeader(response, false);
        if (exHeader != null) {
            for (String key : exHeader.keySet()) {
                if (key.equalsIgnoreCase("location")) {
                    response.setStatus(HttpResponseStatus.FOUND);
                }
                response.headers().set(key, exHeader.getString(key));
            }
        }

        response.content().writeBytes(responseData);
        if (response.headers().get(CONTENT_TYPE) == null) {
            if (responseData.length > 0) {
                response.headers().set(CONTENT_TYPE, Mime.getMime(responseData) + "; charset=UTF-8");
            } else {
                response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
            }
        }
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, TextWebSocketFrame responseData) {
        ctx.channel().writeAndFlush(responseData);
    }

    public static void writeHttpResponse(ChannelHandlerContext ctx, Object responseData) {
        JSONObject exHeader = null;
        //----------流输出
        if (responseData instanceof File) {
            try {
                exHeader = new JSONObject(CONTENT_TYPE.toString(), Mime.getMime((File) responseData));
                responseData = new FileInputStream((File) responseData);
            } catch (Exception e) {
                responseData = rMsg.netMSG(false, "下载文件[" + ((File) responseData).getName() + "]失败");
            }
        }
        if (responseData instanceof InputStream) {//输入流，不管是字符集还是文件
            writeHttpResponse(ctx, (InputStream) responseData, exHeader);
            return;
        }
        //----------字符输出
        if (responseData instanceof byte[]) {
            writeHttpResponse(ctx, (byte[]) responseData, null);
            return;
        }
        //----------301重定向输出
        if (responseData instanceof RpcLocation) {
            location(ctx, responseData.toString());
            return;
        }
        //----------wsText输出
        if (responseData instanceof TextWebSocketFrame) {
            writeHttpResponse(ctx, (TextWebSocketFrame) responseData);
            return;
        }
        //----------字符串转换
        if (!(responseData instanceof String)) {
            responseData = StringHelper.toString(responseData);
        }
        if (responseData != null) {
            responseData = ((String) responseData).getBytes();
            writeHttpResponse(ctx, (byte[]) responseData, JSONObject.build(CONTENT_TYPE.toString(), "text/plain; charset=UTF-8"));
        }
    }
}
