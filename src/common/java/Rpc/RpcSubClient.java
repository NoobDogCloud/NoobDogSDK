package common.java.Rpc;

import common.java.Http.Client.WebSocketClient;
import common.java.Http.Server.HttpContext;
import common.java.Http.Server.SubscribeGsc;
import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RpcSubClient {
    private static final ConcurrentHashMap<String, Consumer<RpcResponse>> topic_receive = new ConcurrentHashMap<>();
    private final WebSocketClient wsc;
    private String topic;
    private String dry_topic;
    private RpcWebSocketQuery query;
    private boolean isSubscribed;       // 是否订阅过了

    private RpcSubClient(String ws_url) {
        wsc = WebSocketClient.build(ws_url).syncConnect();
        // 收到返回数据后分发
        wsc.onReceive(v -> {
            JSONObject ws_result = JSONObject.build(v);
            // 包含 wsId
            if (topic_receive.containsKey(ws_result.getString(HttpContext.GrapeHttpHeader.WebSocket.wsId))) {
                String topic = ws_result.getString(HttpContext.GrapeHttpHeader.WebSocket.wsId);
                topic_receive.get(topic).accept(RpcResponse.build(ws_result));
            }
        });
        // 断线重连后重新订阅
        wsc.onReconnect(ch -> {
            if (isSubscribed) { // 已经订阅过了,需要重新订阅
                if (topic_receive.containsKey(topic)) {
                    subscribe(query, topic_receive.get(topic));
                }
            }
        });
        isSubscribed = false;
    }

    public static RpcSubClient build(String ws_url) {
        return new RpcSubClient((ws_url));
    }

    public RpcSubClient setTopic(String topic) {
        dry_topic = topic;
        return this;
    }

    private String _getTopic(RpcWebSocketQuery query) {
        return StringHelper.isInvalided(dry_topic) ? SubscribeGsc.computerTopic(query.url()) : dry_topic;
    }

    // 订阅服务
    public RpcSubClient subscribe(RpcWebSocketQuery query, Consumer<RpcResponse> receive_fn) {
        // 获得订阅主题
        topic = _getTopic(query);
        if (StringHelper.isInvalided(topic) || receive_fn == null) {
            // 无效主题
            return this;
        }
        query.toSubscribe();
        if (!StringHelper.isInvalided(topic)) {
            query.setTopic(topic);
        }
        send(query);
        // 记录订阅任务
        this.query = query;
        // 记录回调
        topic_receive.put(topic, receive_fn);
        // 记录订阅状态
        isSubscribed = true;
        return this;
    }

    // 取消订阅服务
    public RpcSubClient unsubscribe() {
        if (StringHelper.isInvalided(topic) || query == null) {
            return this;
        }
        query.toUnsubscribe();
        send(query);
        topic_receive.remove(topic);
        return this;
    }

    // 更新订阅对象
    public RpcSubClient update(RpcWebSocketQuery query) {
        query.toUpdate();
        send(query);
        return this;
    }

    private void send(RpcWebSocketQuery query) {
        wsc.send(query.build());
    }
}
