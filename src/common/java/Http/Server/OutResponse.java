package common.java.Http.Server;

import common.java.Http.Mime;
import common.java.Http.Server.Db.HttpContextDb;
import common.java.Rpc.RpcLocation;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.stream.ChunkedStream;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OutResponse {
    private final static int sendBufferLen = 51200;
    private static final String AccessControlAllowHeaders = HttpContext.GrapeHttpHeader.sid + " ," +
            HttpContext.GrapeHttpHeader.token + " ," +
            HttpContext.GrapeHttpHeader.appId + " ," +
            HttpContext.GrapeHttpHeader.publicKey + " ," +
            HttpContextDb.fields + " ," +
            HttpContextDb.sorts + " ," +
            HttpContextDb.options;
    private final ChannelHandlerContext ctx;
    private JSONObject header;
    private FullHttpResponse response;

    private OutResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        header = null;
        response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
    }

    public static OutResponse build(ChannelHandlerContext ctx) {
        return new OutResponse(ctx);
    }

    // -------------------------------------------------------------------
    public static final void defaultOut(ChannelHandlerContext ctx, Object v) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set("Access-Control-Allow-Origin", "*").set("Access-Control-Allow-Headers", AccessControlAllowHeaders);
        response.content().writeBytes(v.toString().getBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static final void defaultRedirect(ChannelHandlerContext ctx, String url) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.setStatus(HttpResponseStatus.FOUND);
        response.headers().set(LOCATION, url);
        response.content().writeBytes("".getBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static final void defaultOptions(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set("Access-Control-Allow-Methods", "GET,POST")
                .set("Access-Control-Max-Age", "86400")
                .set("Access-Control-Allow-Origin", "*")
                .set("Access-Control-Allow-Headers", AccessControlAllowHeaders);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static final void defaultZero(ChannelHandlerContext ctx) {
        defaultOut(ctx, "");
    }

    private void buildHeader(HttpResponse response) {
        HttpHeaders httpHeader = response.headers();
        if (header != null) {
            for (String key : header.keySet()) {
                httpHeader.set(key, header.getString(key));
            }
        }
        httpHeader.set("Access-Control-Allow-Origin", "*")
                .set("Access-Control-Allow-Headers", AccessControlAllowHeaders);
    }

    public void redirect(String url) {
        out(RpcLocation.Instant(url));
    }

    public void out(InputStream v) {
        if (v != null) {
            response.headers().set("Transfer-Encoding", "chunked").set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ChannelFuture sendByteFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(v, sendBufferLen)), ctx.newProgressivePromise());
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            sendByteFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    try {
                        v.close();
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

    public void out(File v) {
        try {
            response.headers().set(CONTENT_TYPE, Mime.getMime(v));
            out(new FileInputStream(v));
        } catch (Exception e) {
            out(rMsg.netMSG(false, "下载文件[" + v.getName() + "]失败"));
        }
    }

    public void out(RpcLocation v) {
        response.setStatus(HttpResponseStatus.FOUND);
        buildHeader(response);
        response.headers().set(LOCATION, v.url());
        response.content().writeBytes("".getBytes());
        out(response);
    }

    public void out(TextWebSocketFrame v) {
        ctx.channel().writeAndFlush(v);
    }

    public void setHeader(JSONObject header) {
        this.header = header;
    }

    public void out(JSONObject v) {
        out(v.toString());
    }

    public void out(JSONArray v) {
        out(v.toString());
    }

    public void out(char v) {
        out(String.valueOf(v));
    }

    public void out(short v) {
        out(String.valueOf(v));
    }

    public void out(boolean v) {
        out(String.valueOf(v));
    }

    public void out(double v) {
        out(String.valueOf(v));
    }

    public void out(float v) {
        out(String.valueOf(v));
    }

    public void out(int v) {
        out(String.valueOf(v));
    }

    public void out(long v) {
        out(String.valueOf(v));
    }

    public void out(String v) {
        out(v.getBytes());
    }

    public void out(Object v) {
        out(StringHelper.toString(v));
    }

    public void out(byte[] v) {
        response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        buildHeader(response);
        response.content().writeBytes(v);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
