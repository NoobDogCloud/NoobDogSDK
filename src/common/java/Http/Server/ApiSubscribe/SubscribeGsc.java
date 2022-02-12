package common.java.Http.Server.ApiSubscribe;

import common.java.DataSource.Subscribe.DistributionSubscribe;
import common.java.DataSource.Subscribe.DistributionSubscribeInterface;
import common.java.DataSource.Subscribe.Room;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 负责处理基于GSC-Websocket请求的服务数据订阅头
 */

public class SubscribeGsc {

    private static final ScheduledExecutorService heart_thread;

    private static DistributionSubscribeInterface distribution_subscribe = null;

    // 定时检测任务
    // 负责 数据新鲜度 同步监测
    // 负责 数据一致性 同步监测
    static {
        heart_thread = Executors.newSingleThreadScheduledExecutor();
        heart_thread.scheduleAtFixedRate(() -> {
            try {
                Room.foreach(room -> {
                    // 需要请求时上下文
                    long n = TimeHelper.getNowTimestampByZero();
                    // 包含需要更新数据
                    if (getUpdateStatus(room)) {
                        // 50ms未动 or 距离上次同步超过500ms => 推送同步数据时间戳
                        if ((n - room.getUpdateTime() > 50) || (n - room.getSyncUpdateTime() > 500)) {
                            _onChanged(room);
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

    /**
     * 注入自定义分布式订阅组件
     */
    public static void injectDistribution(DistributionSubscribeInterface ds) {
        distribution_subscribe = ds;
    }

    /**
     * 注入默认分布式订阅组件
     */
    public static void injectDistribution() {
        distribution_subscribe = new DistributionSubscribe();
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
        // int appId = ctx == null ? 0 : ctx.appId();
        // return (arr.length < (3 + offset) ? null : arr[(1 + offset)] + "#" + arr[(2 + offset)]) + "_" + appId;
        return (arr.length < (3 + offset) ? null : arr[(1 + offset)] + "#" + arr[(2 + offset)]);
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
            var appId = ctx.appId();
            switch (mode) {
                case "subscribe" -> updateOrCreate(topic, appId).add(ctx.channelContext(), ctx);
                case "update" -> update(topic, appId);
                default -> cancel(ctx.channelContext());
            }
        }
        return topic;
    }

    // -----------------------------------------------------------
    // 获得房间对象
    public static Room updateOrCreate(String topic, int appId) {
        return Room.getInstance(topic, appId, distribution_subscribe);
    }

    // 处理主题更新
    public static void update(String topic, int appId) {
        // 标志有新数据,记录数据更新时间
        var room = updateOrCreate(topic, appId);
        room.fleshUpdateStatus().fleshUpdateTime();
        if (distribution_subscribe != null) {
            distribution_subscribe.fleshStatus(room);
        }
    }

    // 处理断开连接或者取消订阅
    private static void cancel(ChannelHandlerContext ch) {
        Room.removeMember(ch.channel().id());
    }

    // 获得主题数据刷新数据
    private static boolean getUpdateStatus(Room room) {
        if (distribution_subscribe != null) {
            Boolean b = distribution_subscribe.pullStatus(room);
            if (b == null) {
                // 如果分布式故障，清空
                nLogger.errorInfo("分布式订阅中间件故障，回退到本地默认订阅模式");
                distribution_subscribe = null;
            }
            return b;
        } else {
            return room.getUpdateStatus();
        }
    }

    private static void _onChanged(Room room) {
        if (distribution_subscribe != null) {
            if (distribution_subscribe.pushStatus(room) == null) {
                nLogger.errorInfo("分布式订阅中间件故障，回退到本地默认订阅模式");
                distribution_subscribe = null;
            }
        }
        room.fleshSyncUpdateTime();
    }
}
