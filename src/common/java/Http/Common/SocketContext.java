package common.java.Http.Common;

import common.java.DataSource.Subscribe.Room;
import common.java.Http.Server.HttpContext;
import common.java.Http.Server.OutResponse;
import common.java.Reflect._reflect;

import java.util.HashMap;

public class SocketContext {
    private static final ThreadLocal<SocketContext> worker = new ThreadLocal<>();
    private final String cid;
    private final HashMap<String, Object> payload;
    private _reflect currentObj;
    private final HashMap<String, Room> subscriber = new HashMap<>();    // 订阅对象
    private HttpContext request;        // 请求对象
    private OutResponse response;       // 应答对象

    private SocketContext(String cid) {
        this.cid = cid;
        this.payload = new HashMap<>();
    }

    public static SocketContext build(String cid) {
        return new SocketContext(cid);
    }

    public static SocketContext build(SocketContext ctx) {
        return ctx.clone();
    }

    public static SocketContext current() {
        return worker.get();
    }

    public SocketContext clone() {
        SocketContext sCtx = new SocketContext(this.cid);
        sCtx.setWorker()
                .setCurrent(this.currentObj)
                .setRequest(this.request)
                .setResponse(this.response);
        for (String key : this.payload.keySet()) {
            sCtx.setValue(key, this.payload.get(key));
        }
        return sCtx;
    }

    public SocketContext setWorker() {
        worker.set(this);
        return this;
    }

    public SocketContext putSubscriber(Room room) {
        if (room != null) {
            subscriber.put(room.getTopicWithAppID(), room);
        }
        return this;
    }

    public SocketContext removeSubscriber(Room room) {
        if (room != null) {
            subscriber.remove(room.getTopicWithAppID());
        }
        return this;
    }

    // 销毁请求上下文时调用它，从线程上下文清除
    public void destroy() {
        // 从所有已订阅房间退出
        for (var room : subscriber.values()) {
            room.leave(cid);
        }
        // 删除上下文
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
