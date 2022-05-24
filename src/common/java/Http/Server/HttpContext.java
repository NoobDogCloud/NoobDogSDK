package common.java.Http.Server;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Config.Config;
import common.java.Encrypt.UrlCode;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.ApiSubscribe.GscSubscribe;
import common.java.Http.Server.Db.HttpContextDb;
import common.java.Number.NumberHelper;
import common.java.Object.ObjectHelper;
import common.java.Rpc.rMsg;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class HttpContext {
    public static final JSONObject methodStore;
    public static final String SessionKey = "HttpContext";
    public static final String payload = "payload";

    static {
        methodStore = new JSONObject();
        methodStore.put("get", Method.get);
        methodStore.put("post", Method.post);
        methodStore.put("websocket", Method.websocket);
    }

    private Method method;

    private HttpRequest request;
    private String absPath;
    private String pathString;
    private String serviceNameString;
    private String classNameString;
    private String actionNameString;
    private String wsID;
    private String svrName;
    private JSONObject parameter;
    private AsciiString mime;
    private ChannelHandlerContext ctx;
    private JSONObject values = new JSONObject();
    private HttpContextDb db_ctx;
    private JSONObject header;

    private JSONObject filter_extends;

    private HttpContext() {
    }

    public HttpContext(ChannelHandlerContext _ctx) {
        ctx = _ctx;
    }

    public HttpContext(HttpRequest _header) {
        initHttpRequest(_header);
    }

    public HttpContext(JSONObject _header) {
        JSONObject headerJson = _header.getJson(GrapeHttpHeader.WebSocket.header);
        if (headerJson != null) {
            for (String key : GrapeHttpHeader.keys) {
                updateValue(headerJson, key);
            }
        }
        parameter(_header.getJson(GrapeHttpHeader.WebSocket.param));
        absPath = _header.getString(GrapeHttpHeader.WebSocket.url);
        updatePath();
        wsID = _header.containsKey(GrapeHttpHeader.WebSocket.wsId) ?
                _header.getString(GrapeHttpHeader.WebSocket.wsId) :
                null;
        method = Method.websocket;
        header = _header;
        init_grape_dbCtx();
    }

    public static HttpContext current() {
        SocketContext sc = SocketContext.current();
        return sc != null ? sc.getRequest() : null;
    }

    public static HttpContext setNewHttpContext() {
        HttpContext httpCtx = new HttpContext();
        SocketContext.current().setValue(HttpContext.SessionKey, httpCtx);
        return httpCtx;
    }

    public static HttpContext newHttpContext() {
        return new HttpContext();
    }

    public HttpRequest getRequest() {
        return request;
    }

    public static void breakRequest() {
        throw new RuntimeException("break request");
    }

    public static void showMessage(ChannelHandlerContext ctx, Object result) {
        showResult(ctx, result);
        if (Config.debug) {
            nLogger.errorInfo(result.toString());
        }
    }

    public static void showResult(ChannelHandlerContext ctx, Object result) {
        if (ctx != null) {
            OutResponse.defaultOut(ctx, rMsg.netMSG(false, result));
            ctx.close();
            ctx.deregister();
        }
    }

    private void init_grape_dbCtx() {
        db_ctx = new HttpContextDb(values);
        // JSONObject db_values = db_ctx.header(values);
        db_ctx.header(values);
    }

    public void initHttpRequest(HttpRequest _header) {
        request = _header;
        // 载入普通header头
        for (String key : GrapeHttpHeader.keys) {
            updateValue(key);
        }
        absPath = _header.uri().trim();
        updatePath();
        method = (Method) methodStore.get(_header.method().name().toLowerCase());
        // 是websocket
        if (method == Method.websocket) {
            for (String key : GrapeHttpHeader.websocketKeys) {
                updateValue(key);
            }
        }
        filter_extends = JSONObject.build();
        init_grape_dbCtx();
    }

    public JSONObject getFilterExtends() {
        return filter_extends;
    }

    public boolean hasExtends(String name) {
        return filter_extends.containsKey(name);
    }

    /**
     * 获得 订阅主题
     */
    public String topic() {
        return values.has(GrapeHttpHeader.WebSocketHeader.wsTopic) ? values.getString(GrapeHttpHeader.WebSocketHeader.wsTopic) : GscSubscribe.computerTopic(this.path());
    }

    /**
     * 获得当前 订阅模式
     */
    public int subscribeMode() {
        // 不包含模式
        if (!values.has(GrapeHttpHeader.WebSocketHeader.wsMode)) {
            return 0;
        }
        return switch (values.getString(GrapeHttpHeader.WebSocketHeader.wsMode)) {
            case "subscribe" -> 1;
            case "cancel" -> 2;
            case "update" -> 3;
            default -> 0;
        };
    }

    public final HttpContext cloneTo() {
        return this.cloneTo(new HttpContext());
    }

    public final HttpContext cloneTo(HttpContext ctx) {
        if (this.request != null) {
            ctx.initHttpRequest(this.request);
        }
        ctx.serviceName(this.svrName);
        ctx.setMime(this.mime);
        ctx.use(this.ctx);
        ctx.method(this.method);
        ctx.path(this.absPath);
        ctx.parameter(this.parameter);
        ctx.headerValues(this.headerValues());
        return ctx;
    }

    private void updatePath() {
        pathString = '/' + StringHelper.build(absPath).trimFrom('/').toString();
        serviceNameString = pathString.split("/")[1];
        classNameString = StringHelper.captureName(pathString.split("/")[2]);
        actionNameString = pathString.split("/")[3];
    }

    public final HttpContext headerValues(JSONObject nheader) {
        this.values = nheader;
        return this;
    }

    public final JSONObject headerValues() {
        return values;
    }

    public final HttpContext method(Method nmh) {
        this.method = nmh;
        return this;
    }

    private HttpContext updateValue(String key) {
        HttpHeaders headers = request.headers();
        if (headers.contains(key)) {
            values.put(key, headers.get(key));
        }
        return this;
    }

    public final AsciiString getMime() {
        return this.mime;
    }

    public final HttpContext setMime(String value) {
        setMime(new AsciiString(value.getBytes()));
        return this;
    }

    public final HttpContext setMime(AsciiString value) {
        this.mime = value;
        return this;
    }

    public final HttpContext serviceName(String svrName) {
        this.svrName = svrName;
        return this;
    }

    public final String serviceName() {
        return this.svrName != null ? this.svrName : serviceNameString;
    }

    public final JSONObject getValues() {
        return values;
    }

    private HttpContext updateValue(JSONObject headers, String key) {
        if (headers.containsKey(key)) {
            values.put(key, headers.get(key));
        }
        return this;
    }

    public JSONObject parameter() {
        return this.parameter;
    }

    public HttpContext parameter(JSONObject p) {
        if (p != null) {
            parameter = p;
        }
        return this;
    }

    public HttpContext path(String path) {
        if (path != null) {
            absPath = path;
            updatePath();
        }
        return this;
    }

    public String getRequestID() {
        return wsID;
    }

    public String path() {
        return pathString;
    }

    public boolean invalidGscRequest() {
        return path().split("/").length < 4;
    }

    /**
     * 获得类名称
     */
    public String className() {
        return classNameString;
    }

    /**
     * 获得方法名称
     */
    public String actionName() {
        return actionNameString;
    }

    public HttpContext use(ChannelHandlerContext _ctx) {
        ctx = _ctx;
        return this;
    }
    /**
     * 获得当前请求对应的订阅主题
     * */


    /**
     * 获得请求方法
     */
    public Method method() {
        return method;
    }

    /**
     * 是否是长连接
     */
    public boolean keepAlive() {
        return HttpUtil.isKeepAlive(request);
    }

    /**
     * 获得host
     */
    public String host() {
        return StringHelper.build(values.getString(GrapeHttpHeader.host)).trimFrom('/').toString();
    }

    public String domain() {
        return host().split(":")[0];
    }

    public int port() {
        String[] arr = StringHelper.build(values.getString(GrapeHttpHeader.host)).trimFrom('/').toString().split(":");
        return arr.length == 2 ? Integer.parseInt(arr[1]) : 80;
    }

    /**
     * 设置host
     */
    public HttpContext host(String newHost) {
        values.put(GrapeHttpHeader.host, newHost);
        return this;
    }

    /**
     * 获得IP
     */
    public String ip() {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    /**
     * 获得sid
     */
    public String sid() {
        return values.getString(GrapeHttpHeader.sid);
    }

    public HttpContext sid(String nSID) {
        values.put(GrapeHttpHeader.sid, nSID);
        return this;
    }

    /**
     * 获得Oauth2授权信息
     */
    public String token() {
        return values.getString(GrapeHttpHeader.token);
    }

    /**
     * 获得浏览器代理头
     */
    public String agent() {
        return values.getString(GrapeHttpHeader.agent);
    }

    /**
     * 获得表单数据
     */
    public String form() {
        return values.getString(GrapeHttpHeader.postPayload);
    }

    /**
     * 获得请求所在APPID
     */

    public int appId() {
        try {
            return NumberHelper.number2int(values.get(GrapeHttpHeader.appId, 0));
        } catch (Exception e) {
        }
        return 0;
    }

    public HttpContext appId(int appId) {
        if (appId > 0) {
            values.put(GrapeHttpHeader.appId, appId);
        }
        return this;
    }

    public String publicKey() {
        return StringHelper.toString(values.get(GrapeHttpHeader.publicKey, null));
    }

    /**
     * 是不是gsc-core该响应的请求
     */
    public boolean isGscRequest() {
        return values.containsKey(GrapeHttpHeader.appId);
    }

    /**
     * 获得参数实例组
     */
    public Object[] invokeParamter() {
        String[] urls = this.path().split("/");
        int offset = 4;
        if (urls.length <= offset) {
            return null;
        }
        // 构造参数组
        Object[] arglist = new Object[urls.length - offset];
        String[] stype;
        String svalue;
        try {
            for (int i = offset; i < urls.length; i++) {
                svalue = UrlCode.decode(urls[i]);
                stype = svalue.split(":");
                int idx = i - offset;
                if (stype.length > 0) {//包含类型信息
                    switch (stype[0].toLowerCase()) {
//string
                        case "s" -> arglist[idx] = svalue.substring(2);

//int
                        case "i" -> arglist[idx] = Integer.parseInt(svalue.substring(2));
                        case "int" -> arglist[idx] = Integer.parseInt(svalue.substring(4));

//char
                        case "char" -> arglist[idx] = svalue.charAt(5);

//short
                        case "short" -> arglist[idx] = Short.parseShort(svalue.substring(6));

//boolean
                        case "b" -> arglist[idx] = Boolean.parseBoolean(svalue.substring(2));
                        case "bool" -> arglist[idx] = Boolean.parseBoolean(svalue.substring(5));

//float
                        case "f" -> arglist[idx] = Float.parseFloat(svalue.substring(2));
                        case "float" -> arglist[idx] = Float.parseFloat(svalue.substring(6));

//long
                        case "l" -> arglist[idx] = Long.parseLong(svalue.substring(2));
                        case "long" -> arglist[idx] = Long.parseLong(svalue.substring(5));

//double
                        case "d" -> arglist[idx] = Double.parseDouble(svalue.substring(2));
                        case "double" -> arglist[idx] = Double.parseDouble(svalue.substring(7));

//json
                        case "j" -> arglist[idx] = JSONObject.build(svalue.substring(2));
                        case "json" -> arglist[idx] = JSONObject.build(svalue.substring(5));

//jsonArray
                        case "ja" -> arglist[idx] = JSONArray.build(svalue.substring(3));
                        case "json_array" -> arglist[idx] = JSONArray.build(svalue.substring(10));

//object
                        case "o" -> arglist[idx] = ObjectHelper.build(svalue.substring(2));
                        case "object" -> arglist[idx] = ObjectHelper.build(svalue.substring(7));
                        default -> arglist[idx] = svalue;
                    }
                } else {
                    arglist[i] = svalue;
                }
            }
        } catch (Exception e) {
            nLogger.errorInfo(e, "非法输入参数!(疑似接口分析)");
            arglist = null;
        }
        return arglist;
    }

    public final String url() {
        return host() + path();
    }

    /**
     * 获得上下文频道
     */
    public Channel channel() {
        return ctx.channel();
    }

    /**
     * 获得上下文
     */
    public ChannelHandlerContext channelContext() {
        return ctx;
    }

    /**
     * 获得jsonobject header对象
     */
    public JSONObject header() {
        if (header != null) {
            return header.getJson("header");
        } else {
            JSONObject nHeader = new JSONObject();
            getValueSafe(GrapeHttpHeader.sid, nHeader);
            getValueSafe(GrapeHttpHeader.token, nHeader);
            getValueSafe(GrapeHttpHeader.appId, nHeader);
            return nHeader;
        }
    }

    /**
     * 设置jsonobject header对象
     */
    public HttpContext header(JSONObject nHeader) {
        setValueSafe(GrapeHttpHeader.sid, nHeader);
        setValueSafe(GrapeHttpHeader.token, nHeader);
        setValueSafe(GrapeHttpHeader.appId, nHeader);
        return this;
    }

    public HttpContextDb dbHeaderContext() {
        return db_ctx;
    }

    /**
     * 任意地点抛出返回值
     */
    public void throwOut(String msg) {
        HttpContext.showMessage(this.channelContext(), msg);
        throw new RuntimeException(msg);
    }

    /**
     * 调试模式时,抛出异常
     */
    public void throwDebugOut(String msg) {
        if (MicroServiceContext.current().isDebug()) {
            HttpContext.showMessage(this.channelContext(), msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * 调试模式时,抛出异常
     */
    public void out(Object msg) {
        HttpContext.showResult(this.channelContext(), msg);
    }

    public Object payload() {
        return parameter != null ? parameter.get(payload) : null;
    }

    /**
     * 转化 http header 为 json
     */
    public JSONObject toJson() {
        return JSONObject.build()
                .put(GrapeHttpHeader.WebSocket.url, this.absPath)
                .put(GrapeHttpHeader.WebSocket.header, this.values)
                .put(GrapeHttpHeader.WebSocket.param, this.parameter);
    }

    public String toString() {
        return toJson().toString();
    }

    private HttpContext setValueSafe(String key, JSONObject nHeader) {
        if (nHeader.containsKey(key)) {
            values.put(key, nHeader.get(key));
        }
        return this;
    }

    private HttpContext getValueSafe(String key, JSONObject c) {
        if (values.containsKey(key)) {
            c.put(key, values.get(key));
        }
        return this;
    }


    public enum Method {
        get, post, websocket
    }

    public static class GrapeHttpHeader {

        public final static String ip = "ip";
        public final static String sid = "GrapeSID";
        public final static String token = "GrapeOauth2";
        public final static String host = "host";
        public final static String agent = "agent";
        public final static String postPayload = "exData";
        public final static String appId = "appID";
        public final static String publicKey = "appKey";        // 请求公钥

        public final static String ChannelContext = "socket";
        public final static List<String> keys = new ArrayList<>();
        public final static List<String> apps = new ArrayList<>();
        public final static List<String> xmls = new ArrayList<>();
        public final static List<String> websocket = new ArrayList<>();
        public final static List<String> websocketKeys = new ArrayList<>();

        static {
            keys.add(GrapeHttpHeader.ip);
            keys.add(GrapeHttpHeader.sid);
            keys.add(GrapeHttpHeader.token);
            keys.add(GrapeHttpHeader.host);
            keys.add(GrapeHttpHeader.agent);
            keys.add(GrapeHttpHeader.postPayload);
            keys.add(GrapeHttpHeader.appId);
            keys.add(GrapeHttpHeader.ChannelContext);
            keys.add(GrapeHttpHeader.publicKey);

            //db
            keys.add(HttpContextDb.fields);
            keys.add(HttpContextDb.sorts);
            keys.add(HttpContextDb.options);

            //app
            apps.add(App.fullUrl);

            //WebSocket
            websocket.add(WebSocket.url);
            websocket.add(WebSocket.header);
            websocket.add(WebSocket.param);
            websocket.add(WebSocket.wsId);

            // WebSocketHeader
            websocketKeys.add(WebSocketHeader.wsMode);
            websocketKeys.add(WebSocketHeader.wsTopic);

            //Xml
            xmls.add(payload);
        }

        public static class WebSocket {
            public final static String url = "path";
            public final static String header = "header";
            public final static String param = "param";
            public final static String wsId = "wsID";
        }

        public static class WebSocketHeader {
            // WS请求额外定义
            public static final String wsMode = "mode";
            // WS topic定义
            public static final String wsTopic = "topic";
        }

        public static class App {
            public final static String fullUrl = "requrl";
        }
    }

}
