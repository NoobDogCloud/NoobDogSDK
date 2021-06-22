package common.java.HttpServer;

import common.java.HttpServer.Subscribe.Room;
import common.java.Time.TimeHelper;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 负责处理基于GSC-Websocket请求的服务数据订阅头
 */

public class SubscribeGsc {

    private static final ScheduledExecutorService heart_thread;
    // 注册更新广播回调（支持本地或者通过redis跨机器广播）
    // 主题数据改变后50ms内不更新->更新主题更新时间
    // 主题数据发生改变500ms内->更新主题更新时间
    // 主题数据500ms内一直更新,最多2s->更新主题更新时间
    private static Consumer<String> onChanged;          // 推送当前主题更新时间
    // 获得主题当前数据刷新时间（新鲜值）
    private static Function<String, Long> getFlesh;     // 拉取当前主题更新时间

    public static void setOnChanged(Consumer<String> onChanged) {
        SubscribeGsc.onChanged = onChanged;
    }

    public static void setGetFlesh(Function<String, Long> getFlesh) {
        SubscribeGsc.getFlesh = getFlesh;
    }

    // 定时检测任务
    // 负责 数据新鲜度 同步监测
    // 负责 数据一致性 同步监测
    static {
        heart_thread = Executors.newSingleThreadScheduledExecutor();
        heart_thread.scheduleAtFixedRate(() -> {
            Room.foreach(room -> {
                String topic = room.getTopic();
                long n = TimeHelper.getNowTimestampByZero();
                // 同步新鲜度检测(最小同步时间50ms)
                if (n - room.getUpdateTime() > 50) {
                    _onChanged(topic);
                    room.fleshUpdateTime();
                }
                // 同步新鲜度检测(最大同步时间500ms)
                if (n - room.getSyncUpdateTime() > 500) {
                    // 上次同步后，又发生了数据更新，超过500ms未处理
                    if (room.getUpdateTime() > room.getSyncUpdateTime()) {
                        _onChanged(topic);
                        room.fleshSyncUpdateTime();
                    }
                }
                // 数据一致性检测
                if (n - room.getBroadcastTime() > 2000) {
                    // 数据更新时间晚于广播时间
                    if (SubscribeGsc.getUpdateTime(topic) > room.getBroadcastTime()) {
                        // 刷新房间内所有用户数据
                        room.update();
                    }
                }
            });
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private static String getAutoTopic(HttpContext ctx) {
        // 正常情况
        String[] arr = ctx.path().split("/");
        if (arr.length < 3) {
            HttpContext.current().throwOut("请求参数异常!");
        }
        return arr[1] + "#" + arr[2];
    }

    // 根据gsc-websocket请求体计算topic字符串
    private static String getTopic(HttpContext ctx) {
        String topic = "";
        JSONObject header = ctx.header();
        if (header != null) {
            if (header.containsKey(HttpContext.GrapeHttpHeader.WebSocket.wsTopic)) {
                topic = header.getString(HttpContext.GrapeHttpHeader.WebSocket.wsTopic);
            }
        }
        //  +topic 定义 or topic 定义 并 appId
        return (topic.length() == 0 ? getAutoTopic(ctx) :
                (topic.startsWith("+")) ? getAutoTopic(ctx) + topic : topic) + "_" + ctx.appId();
    }

    // 订阅参数过滤
    public static String filterSubscribe(HttpContext ctx) {
        JSONObject h = ctx.header();
        String topic = getTopic(ctx);
        if (h.containsKey("mode")) {
            String mode = h.getString("mode");
            switch (mode) {
                case "subscribe":
                    updateOrCreate(topic);
                    break;
                case "update":
                    update(topic);
                    break;
                default:
                    cancel(ctx.channelContext());
            }
        }
        return topic;
    }

    // -----------------------------------------------------------
    // 获得房间对象
    private static Room updateOrCreate(String topic) {
        return Room.getInstance(topic);
    }

    // 处理主题更新
    private static void update(String topic) {
        // 刷新主题数据更新时间戳
        updateOrCreate(topic).fleshUpdateTime();
    }

    // 处理断开连接或者取消订阅
    private static void cancel(ChannelHandlerContext ch) {
        Room.removeMember(ch.channel().id());
    }

    // 获得主题数据刷新数据
    private static long getUpdateTime(String topic) {
        return (getFlesh != null) ? getFlesh.apply(topic) : Room.getInstance(topic).getUpdateTime();
    }

    private static void _onChanged(String topic) {
        if (SubscribeGsc.onChanged != null) {
            SubscribeGsc.onChanged.accept(topic);
        }
    }
}
