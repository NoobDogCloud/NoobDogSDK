package common.java.Http.Server;

import common.java.Encrypt.UrlCode;
import common.java.File.FileHelper;
import common.java.Http.Common.RequestSession;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.Upload.UploadFileInfo;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import common.java.Xml.XmlHelper;
import common.java.nLogger.nLogger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.json.gsc.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class NetEvents extends ChannelInboundHandlerAdapter {
    private HttpRequest _req;

    public static String tryfixURL(String content) {
        var arr = StringHelper.path2list(content);
        arr.replaceAll(UrlCode::encode);
        return StringHelper.join(arr, "/");
    }

    public static JSONObject PostContent2JSON(String _httpContent) {
        JSONObject rString = null;
        try {
            String httpContent = _httpContent;
            if (httpContent != null) {
                httpContent = UrlCode.decode(httpContent);
                rString = StringHelper.path2rpc(httpContent);//get风格请求转化
                if (null == rString) {
                    rString = JSONObject.toJSON(httpContent);//json风格转换
                    if (null == rString) {
                        rString = XmlHelper.toJson(httpContent);//xml 风格转换,同时附加原始数据
                        /*
                        if (rString != null) {
                            rString.put("xmldata", httpContent);//保留原始数据
                        }
                        */
                    }
                }
            }
            rString.put(HttpContext.payload, httpContent);
        } catch (Exception e) {
            rString = null;
        }
        return rString;
    }

    public Object _Hook(ChannelHandlerContext ctx, String url) {
        String[] gr = UrlCode.decode(StringHelper.build(url).trimFrom('/').toString().split("/"));
        Object rs = null;
        try {
            if ("@redirect".equalsIgnoreCase(gr[0])) {
                if (gr.length == 2) {
                    OutResponse.defaultRedirect(ctx, url);
                }
                rs = "";
            }
        } catch (Exception e) {
            rs = "调用参数类型异常";
        }
        return rs;
    }

    //传统get参数转换成内部参数
    private QueryStringDecoder isNotGscGet(String uri) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        return decoder.parameters().size() > 0 ? decoder : null;
    }

    // 将请求格式是 gsc-rpc 的post转化成等同的get
    private static String fixPostBody(String _url, String tempBody) {
        try {
            String appendURL = tryfixURL(tempBody);
            if (!appendURL.equals("") && !StringHelper.isInvalided(appendURL)) {
                _url += "/" + appendURL;
            }
            return _url;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String filterURLencodeWord(String url) {
        return UrlCode.decode(url);
    }

    private boolean isGscPost(String str) {
        if (StringHelper.isInvalided(str)) {
            return false;
        }
        return str.startsWith("gsc-post:");
    }

    private String convertByteBufToString(ByteBuf buffer) {
        String rString;
        byte[] req = new byte[buffer.readableBytes()];
        buffer.readBytes(req);
        rString = new String(req, StandardCharsets.UTF_8);
        return rString;
    }

    /**
     * 包含头部信息的get
     */
    private String updateHeaderFromURL(String path) {
        String[] paths = StringHelper.build(path).trimFrom('/').toString().split("/");
        if (paths[0].equalsIgnoreCase("gscheader_start")) {
            int end_idx = 0;
            for (int i = 0; i < paths.length; i++) {
                if (paths[i].equalsIgnoreCase("gscheader_end")) {
                    end_idx = i;
                    break;
                }
            }
            if (end_idx > 0) {
                HttpHeaders h = _req.headers();
                for (int l = 1; l < end_idx; l += 2) {
                    h.add(paths[l], paths[l + 1]);
                }
                path = "/" + StringHelper.join(paths, "/", end_idx + 1, -1);
            }
        }
        return path;
    }

    private void webSocket(ChannelHandlerContext _ctx, TextWebSocketFrame msg) {
        var b = msg.content();
        try {
            String wsData = convertByteBufToString(b.copy());  //将数据按照utf-8的方式转化为字符串
            JSONObject json = JSONObject.toJSON(wsData);
            if (JSONObject.isInvalided(json) || !json.containsKey("path") || !json.containsKey("header")) {
                OutResponse.defaultOut(_ctx, rMsg.netMSG(false, "请求错误!"));
            } else {
                String body = json.getString("param");
                if (!StringHelper.isInvalided(body)) {
                    String _url = filterURLencodeWord(json.getString("path"));
                    if (isGscPost(body)) {
                        _url = NetEvents.fixPostBody(_url, body);
                    }
                    if (_url != null) {
                        // 重设uri地址
                        json.put("path", _url);
                        nLogger.debugInfo("websocket-gsc-post:" + _url);
                    }
                }
                // 开始服务
                GrapeHttpServer.startService(json, _ctx, null);
            }
        } catch (Exception e) {
            nLogger.errorInfo(e, "WebSocket通讯异常");
        } finally {
            // b.release();
            ReferenceCountUtil.release(b);
        }
    }

    private void httpRequest(ChannelHandlerContext _ctx, HttpContent msg) {
        JSONObject postParam = null;
        String _url = filterURLencodeWord(_req.uri());
        boolean vaild = false;
        if (_req.method().equals(HttpMethod.POST)) {
            var b = msg.content();
            try {
                String tempBody = convertByteBufToString(b.copy());
                // 是gsc-post
                if (isGscPost(tempBody)) {
                    // 将请求格式是 gsc-rpc 的post转化成等同的get
                    _url = NetEvents.fixPostBody(_url, tempBody);
                    nLogger.debugInfo("gsc-post:" + _url);
                } else {
                    // 分析正常post请求参数
                    postParam = PostParameter(msg);
                    // 不是表单
                    if (postParam == null) {
                        postParam = PostContent2JSON(tempBody);
                    }
                    nLogger.debugInfo("post:" + _url);
                }
                vaild = true;
            } catch (Exception e) {
                nLogger.errorInfo(e, "通讯异常");
            } finally {
                b.release();
            }
        }
        if (_req.method().equals(HttpMethod.GET)) {
            QueryStringDecoder decoder = isNotGscGet(_url);
            // 不是普通gsc-get请求
            if (decoder != null) {
                JSONObject _normalGetParameter = new JSONObject();
                decoder.parameters().forEach((key, values) -> _normalGetParameter.put(key, values.get(0)));
                postParam = _normalGetParameter;
                _url = decoder.path();
                nLogger.debugInfo("normal-get:" + _url);
            } else {
                nLogger.debugInfo("gsc-get:" + _url);
                // 判断是不是带上各类header参数的GET请求
                _url = updateHeaderFromURL(_url);
            }
            vaild = true;
        }
        if (vaild) {
            // 重设uri地址
            if (_url != null) {
                _req.setUri(_url);
            }
            GrapeHttpServer.startService(_req, _ctx, postParam);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext _ctx, Object msg) throws Exception {
        //Connection: Upgrade
        if (msg instanceof HttpRequest) {
            _req = (HttpRequest) msg;
        }
        if (_req != null) {
            if (_req.uri().equals("/favicon.ico")) {
                OutResponse.defaultZero(_ctx);
                return;
            }

            if (_req.uri().equals("/@heart")) {
                OutResponse.defaultOut(_ctx, "1");
                return;
            }

            HttpMethod method = _req.method();
            if (method.equals(HttpMethod.OPTIONS)) {
                OutResponse.defaultOptions(_ctx);
                return;
            }

            if (!method.equals(HttpMethod.GET) && !method.equals(HttpMethod.POST)) {
                OutResponse.defaultZero(_ctx);
                return;
            }

            if (_Hook(_ctx, _req.uri()) != null) {//过滤执行
                OutResponse.defaultZero(_ctx);
                return;
            }
        }
        if (msg instanceof HttpContent) {
            httpRequest(_ctx, (HttpContent) msg);
        }
        if (msg instanceof TextWebSocketFrame) {//文本ws
            webSocket(_ctx, (TextWebSocketFrame) msg);
        }
    }

    private JSONObject PostParameter(Object req) {
        JSONObject parmMap = new JSONObject();
        try {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(_req);
            decoder.offer((HttpContent) req);
            try {
                while (decoder.hasNext()) {
                    InterfaceHttpData data = decoder.next();
                    if (data != null) {
                        try {
                            if (data.getHttpDataType() == HttpDataType.Attribute || data.getHttpDataType() == HttpDataType.InternalAttribute) {
                                Attribute _data = (Attribute) data;
                                try {
                                    parmMap.put(_data.getName(), _data.getValue());
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    // nLogger.debugInfo(e);
                                }
                                continue;
                            }
                            if (data.getHttpDataType() == HttpDataType.FileUpload) {
                                //String uploadFileName = getUploadFileName(data);
                                FileUpload _data = (FileUpload) data;
                                if (_data.isCompleted()) {
                                    //System.err.println(_data.getName());
                                    UploadFileInfo fileInfo = new UploadFileInfo(_data.getFilename(), _data.getContentType(), _data.getMaxSize());
                                    if (_data.isInMemory()) {
                                        try {
                                            fileInfo.append(_data.getByteBuf().copy());
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            // nLogger.debugInfo(e);
                                        }
                                    } else {
                                        try {
                                            File newTempFile = new File(FileHelper.buildTempFile());
                                            _data.getFile().renameTo(newTempFile);
                                            fileInfo.append(newTempFile);
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            // nLogger.debugInfo(e);
                                        }
                                    }
                                    parmMap.put(_data.getName(), fileInfo);
                                }
                            }
                        } catch (Exception e) {
                            // nLogger.debugInfo(e);
                        } finally {
                            data.release();
                        }
                    }
                }
            } catch (EndOfDataDecoderException e1) {
                // nLogger.debugInfo(e1);
            } finally {
                decoder.destroy();
            }
        } catch (Exception e) {
        }

        return parmMap.isEmpty() ? null : parmMap;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        RequestSession.create(ctx.channel().id().asLongText());
        try {
            super.channelActive(ctx);
        } catch (Exception e) {
            nLogger.debugInfo(e);
        }
    }

    private void destroySocketContext(String cid) {
        SocketContext sc = RequestSession.get(cid);
        if (sc != null) {
            sc.destroy();
        }
        RequestSession.remove(cid);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        destroySocketContext(ctx.channel().id().asLongText());
        try {
            super.channelInactive(ctx);
        } catch (Exception e) {
            nLogger.debugInfo(e);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        destroySocketContext(ctx.channel().id().asLongText());
        ctx.close();
        /*
        try {
            super.exceptionCaught(ctx, cause);
        } catch (Exception e) {
            nLogger.debugInfo(e);
        }
         */
        if (ctx.channel().isActive()) {
            // sendError(ctx, INTERNAL_SERVER_ERROR);
            nLogger.errorInfo("INTERNAL_SERVER_ERROR");
        }
    }

}
