package common.java.Rpc;

import common.java.Http.Server.HttpContext;
import org.json.gsc.JSONObject;

public record RpcWebSocketQuery(String url, JSONObject header) {
    public static RpcWebSocketQuery build(String url, JSONObject header) {
        return new RpcWebSocketQuery(url, header);
    }

    public JSONObject build() {
        return JSONObject.build().put("path", url).put("header", header).put("param", "{}");
    }

    // 设置订阅模式
    public RpcWebSocketQuery toSubscribe() {
        header.put(HttpContext.GrapeHttpHeader.WebSocket.wsMode, "subscribe");
        return this;
    }

    // 设置更新模式
    public RpcWebSocketQuery toUpdate() {
        header.put(HttpContext.GrapeHttpHeader.WebSocket.wsMode, "update");
        return this;
    }

    // 设置取消订阅模式
    public RpcWebSocketQuery toUnsubscribe() {
        header.put(HttpContext.GrapeHttpHeader.WebSocket.wsMode, "cancel");
        return this;
    }

    // 设置自定义主题
    public RpcWebSocketQuery setTopic(String topic) {
        header.put(HttpContext.GrapeHttpHeader.WebSocket.wsTopic, topic);
        return this;
    }
}
