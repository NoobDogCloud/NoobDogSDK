package common.java.Http.Server;

import common.java.Http.Server.Subscribe.Room;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
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
    private static Function<String, Boolean> getFleshStatus;     // 拉取当前主题更新时间

    public static void setOnChanged(Consumer<String> onChanged) {
        SubscribeGsc.onChanged = onChanged;
    }

    // 定时检测任务
    // 负责 数据新鲜度 同步监测
    // 负责 数据一致性 同步监测
    static {
        heart_thread = Executors.newSingleThreadScheduledExecutor();
        heart_thread.scheduleAtFixedRate(() -> {
            try {
                Room.foreach(room -> {
                    String topic = room.getTopic();
                    long n = TimeHelper.getNowTimestampByZero();
                    // 包含需要更新数据
                    if (getUpdateStatus(topic)) {
                        // 50ms未动 or 距离上次同步超过500ms => 推送同步数据时间戳
                        if ((n - room.getUpdateTime() > 50) || (n - room.getSyncUpdateTime() > 500)) {
                            _onChanged(topic, room);
                        }
                        // 距离上次广播数据超过1000ms
                        if (n - room.getBroadcastTime() > 1000) {
                            // 刷新房间内所有用户数据
                            room.update();
                        }
                    }
                });
            } catch (Exception e) {
                nLogger.logInfo(e);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    public static void setGetFleshStatus(Function<String, Boolean> getFleshStatus) {
        SubscribeGsc.getFleshStatus = getFleshStatus;
    }

    public static String computerTopic(String path) {
        String _path = path;
        int offset = 0;
        // 包含Host
        if (path.indexOf("://") > 0) {
            _path = path.split("://")[1];
            offset = 1;
        }
        String[] arr = _path.split("/");
        HttpContext ctx = HttpContext.current();
        int appId = ctx == null ? 0 : ctx.appId();
        return (arr.length < (3 + offset) ? null : arr[(1 + offset)] + "#" + arr[(2 + offset)]) + "_" + appId;
    }

    private static String getAutoTopic(HttpContext ctx) {
        String t = computerTopic(ctx.path());
        // 正常情况
        if (StringHelper.isInvalided(t)) {
            HttpContext.current().throwOut("请求参数异常!");
        }
        return t;
    }

    // 根据gsc-websocket请求体计算topic字符串
    private static String getTopic(HttpContext ctx) {
        String topic = "";
        JSONObject header = ctx.header();
        if (header != null) {
            if (header.containsKey(HttpContext.GrapeHttpHeader.WebSocketHeader.wsTopic)) {
                topic = header.getString(HttpContext.GrapeHttpHeader.WebSocketHeader.wsTopic);
            }
        }
        //  +topic 定义 or topic 定义 并 appId
        return (topic.length() == 0 ? getAutoTopic(ctx) :
                (topic.startsWith("+")) ? getAutoTopic(ctx) + topic : topic);
    }

    // 订阅参数过滤
    public static String filterSubscribe(HttpContext ctx) {
        JSONObject h = ctx.header();
        String topic = getTopic(ctx);
        if (h.containsKey("mode")) {
            String mode = h.getString("mode");
            switch (mode) {
                case "subscribe":
                    updateOrCreate(topic).add(ctx.channelContext(), ctx);
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
        // 标志有新数据,记录数据更新时间
        updateOrCreate(topic).fleshUpdateStatus().fleshUpdateTime();
    }

    // 处理断开连接或者取消订阅
    private static void cancel(ChannelHandlerContext ch) {
        Room.removeMember(ch.channel().id());
    }

    // 获得主题数据刷新数据
    private static boolean getUpdateStatus(String topic) {
        return (getFleshStatus != null) ? getFleshStatus.apply(topic) : Room.getInstance(topic).getUpdateStatus();
    }

    private static void _onChanged(String topic, Room room) {
        if (SubscribeGsc.onChanged != null) {
            SubscribeGsc.onChanged.accept(topic);
        }
        room.fleshSyncUpdateTime();
    }
}
