package common.java.Http.Common;

import common.java.Http.Server.HttpContext;
import common.java.Http.Server.OutResponse;
import common.java.Reflect._reflect;

import java.util.HashMap;

public class SocketContext {
    private static final ThreadLocal<SocketContext> worker = new ThreadLocal<>();
    private final String cid;
    private final HashMap<String, Object> payload;
    private _reflect currentObj;
    private HttpContext request;
    private OutResponse response;

    private SocketContext(String cid) {
        this.cid = cid;
        this.payload = new HashMap<>();
    }

    public static SocketContext build(String cid) {
        return new SocketContext(cid);
    }

    public static SocketContext current() {
        return worker.get();
    }

    public SocketContext setWorker() {
        worker.set(this);
        return this;
    }

    // 销毁请求上下文时调用它，从线程上下文清除
    public void destroy() {
        worker.remove();
    }

    public SocketContext setCurrent(_reflect _currentClass) {
        currentObj = _currentClass;
        return this;
    }

    public _reflect getCurrentClass() {
        return currentObj;
    }

    public <T> T getValue(String key) {
        return (T) payload.get(key);
    }

    public <T> SocketContext setValue(String key, T val) {
        payload.put(key, val);
        return this;
    }

    public HttpContext getRequest() {
        return request;
    }

    public SocketContext setRequest(HttpContext request) {
        this.request = request;
        return this;
    }

    public OutResponse getResponse() {
        return response;
    }

    public SocketContext setResponse(OutResponse response) {
        this.response = response;
        return this;
    }

    public String getChannelID() {
        return cid;
    }
}