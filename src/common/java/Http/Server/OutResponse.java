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

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OutResponse {
    private final static int sendBufferLen = 5120;
    private static final String AccessControlAllowHeaders = HttpContext.GrapeHttpHeader.sid + " ," +
            HttpContext.GrapeHttpHeader.token + " ," +
            HttpContext.GrapeHttpHeader.appId + " ," +
            HttpContext.GrapeHttpHeader.publicKey + " ," +
            HttpContext.GrapeHttpHeader.WebSocketHeader.wsTopic + " ," +
            HttpContextDb.fields + " ," +
            HttpContextDb.sorts + " ," +
            HttpContextDb.options + " ," +
            CONTENT_TYPE;
    private final ChannelHandlerContext ctx;
    private JSONObject header;
    // private HttpResponse response;

    private OutResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        header = null;
        // response = new DefaultHttpResponse(HTTP_1_1, OK);
    }

    public static OutResponse build(ChannelHandlerContext ctx) {
        return new OutResponse(ctx);
    }

    // -------------------------------------------------------------------
    public static void defaultOut(ChannelHandlerContext ctx, Object v) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers()
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .set(ACCESS_CONTROL_MAX_AGE, "86400")
                .set(ACCESS_CONTROL_ALLOW_HEADERS, AccessControlAllowHeaders);
        response.content().writeBytes(v.toString().getBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void defaultRedirect(ChannelHandlerContext ctx, String url) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.setStatus(HttpResponseStatus.FOUND);
        response.headers().set(LOCATION, url);
        response.content().writeBytes("".getBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void defaultOptions(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers()
                .set(ACCESS_CONTROL_ALLOW_METHODS, "GET,POST")
                .set(ACCESS_CONTROL_MAX_AGE, "86400")
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .set(ACCESS_CONTROL_ALLOW_HEADERS, AccessControlAllowHeaders);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void defaultZero(ChannelHandlerContext ctx) {
        defaultOut(ctx, "");
    }

    public void setHeader(JSONObject header) {
        this.header = header;
    }

    private void buildHeader(HttpResponse response, int length) {
        HttpHeaders httpHeader = response.headers();
        if (header != null) {
            for (String key : header.keySet()) {
                httpHeader.set(key, header.getString(key));
            }
        }
        httpHeader
                .set(CONTENT_LENGTH, length)
                .set(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .set(ACCESS_CONTROL_MAX_AGE, "86400")
                .set(ACCESS_CONTROL_ALLOW_HEADERS, AccessControlAllowHeaders);
    }

    public void redirect(String url) {
        out(RpcLocation.Instant(url));
    }

    public void out(InputStream v, HttpResponse response) {
        if (v != null) {
            response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED); //.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(response);
            ChannelFuture sendByteFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(v, sendBufferLen)), ctx.newProgressivePromise());
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            sendByteFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                    if (total < 0) { // total unknown
                        System.err.println(future.channel() + " Transfer progress: " + progress);
                    } else {
                        System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                    }
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    try {
                        var e = future.cause();
                        if (e != null) {
                            e.printStackTrace();
                        }
                        v.close();
                    } catch (Exception e) {
                        nLogger.logInfo("steam is closed");
                    }
                }
            });
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            // sendByteFuture.addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void out(ByteArrayInputStream v) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        buildHeader(response, v.available());
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        out(v, response);
    }

    public void out(FileInputStream v) throws IOException {
        var ch = v.getChannel();
        try {
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            int length = (int) ch.size();
            buildHeader(response, length);
            // 获得文件前 50 字节
            byte[] bytes = new byte[50];
            v.read(bytes, 0, Math.min(length, 50));
            ch.position(0);
            // application/octet-stream
            String mineType = Mime.getMime(bytes);
            response.headers().set(CONTENT_TYPE, mineType);
            out(v, response);
        } catch (IOException e) {
            e.printStackTrace();
            defaultZero(ctx);
        }
    }

    public void out(File v) {
        try {
            out(new FileInputStream(v));
        } catch (Exception e) {
            out(rMsg.netMSG(false, "下载文件[" + v.getName() + "]失败"));
        }
    }

    public void out(RpcLocation v) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.setStatus(HttpResponseStatus.FOUND);
        buildHeader(response, 0);
        response.headers().set(LOCATION, v.url());
        response.content().writeBytes("".getBytes());
        out(response);
    }

    public void out(TextWebSocketFrame v) {
        ctx.channel().writeAndFlush(v);
    }

    public void out(HashMap<?, ?> v) {
        out(v.toString());
    }

    public void out(List<?> v) {
        out(v.toString());
    }

    public void out(Set<?> v) {
        out(v.toString());
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

    public void out(byte[] v) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        buildHeader(response, v.length);
        response.content().writeBytes(v);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public void out(Object v) {
        if (v instanceof String) {
            out((String) v);
        } else if (v instanceof JSONObject) {
            out((JSONObject) v);
        } else if (v instanceof JSONArray) {
            out((List<?>) v);
        } else if (v instanceof TextWebSocketFrame) {
            out((TextWebSocketFrame) v);
        } else if (v instanceof Boolean) {
            out((boolean) v);
        } else if (v instanceof Double) {
            out((double) v);
        } else if (v instanceof Float) {
            out((float) v);
        } else if (v instanceof Integer) {
            out((int) v);
        } else if (v instanceof Long) {
            out((long) v);
        } else if (v instanceof HashMap) {
            out((HashMap<?, ?>) v);
        } else if (v instanceof List) {
            out((List<?>) v);
        } else if (v instanceof Set) {
            out((Set<?>) v);
        } else if (v instanceof File) {
            out((File) v);
        } else if (v instanceof InputStream) {
            out(v);
        } else if (v instanceof RpcLocation) {
            out((RpcLocation) v);
        } else if (v instanceof byte[]) {
            out((byte[]) v);
        } else if (v instanceof Character) {
            out((char) v);
        } else if (v instanceof Short) {
            out((short) v);
        } else {
            out(StringHelper.toString(v));
        }
    }
}
