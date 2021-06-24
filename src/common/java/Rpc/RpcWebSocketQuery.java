package common.java.Rpc;

import common.java.Http.Server.HttpContext;
import org.json.gsc.JSONObject;

public record RpcWebSocketQuery(String url, JSONObject header) {
    public static RpcWebSocketQuery build(String url, JSONObject header) {
        return new RpcWebSocketQuery(url, header);
    }

    public JSONObject build() {
        if (url.indexOf("://") >= 0) {   // url包含协议
            throw new RuntimeException("输入url[" + url + "]是完整请求url,仅允许输入path");
        }
        return JSONObject.build().put("path", url).put("header", header).put("param", "{}");
    }

    // 设置订阅模式
    public RpcWebSocketQuery toSubscribe() {
        header.put(HttpContext.GrapeHttpHeader.WebSocketHeader.wsMode, "subscribe");
        return this;
    }

    // 设置更新模式
    public RpcWebSocketQuery toUpdate() {
        header.put(HttpContext.GrapeHttpHeader.WebSocketHeader.wsMode, "update");
        return this;
    }

    // 设置取消订阅模式
    public RpcWebSocketQuery toUnsubscribe() {
        header.put(HttpContext.GrapeHttpHeader.WebSocketHeader.wsMode, "cancel");
        return this;
    }

    // 设置自定义主题
    public RpcWebSocketQuery setTopic(String topic) {
        header.put(HttpContext.GrapeHttpHeader.WebSocketHeader.wsTopic, topic);
        return this;
    }
}
