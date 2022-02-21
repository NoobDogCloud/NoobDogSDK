package common.java.Http.Server.ApiSubscribe;

import common.java.DataSource.Subscribe.DistributionSubscribe;
import common.java.DataSource.Subscribe.DistributionSubscribeInterface;
import common.java.DataSource.Subscribe.Room;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;

/**
 * 负责处理基于GSC-Websocket请求的服务数据订阅头
 */

public class GscSubscribe {
    private static DistributionSubscribeInterface distribution_subscribe = null;
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
    public static String filterSubscribe(SocketContext sCtx) {
        HttpContext ctx = sCtx.getRequest();
        JSONObject h = ctx.header();
        String topic = getTopic(ctx);
        if (h.containsKey("mode")) {
            String mode = h.getString("mode");
            var appId = ctx.appId();
            switch (mode) {
                case "subscribe" -> {
                    updateOrCreate(topic, appId).add(ctx.channelContext(), sCtx);
                }
                case "update" -> update(topic, appId);
                case "cancel" -> cancel(ctx.channelContext());
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
        update(updateOrCreate(topic, appId));
    }
    public static void update(Room room) {
        room.fleshUpdateStatus().fleshUpdateTime();
        if (distribution_subscribe != null) {
            distribution_subscribe.fleshStatus(room);
        }
    }

    // 处理断开连接或者取消订阅
    private static void cancel(ChannelHandlerContext ch) {
        Room.removeMember(ch.channel().id());
    }

    // 删除数据源
    public static void remove(Room room) {
        if (distribution_subscribe != null) {
            distribution_subscribe.removeStatus(room);
        }
        room.releaseRoom();
    }

    public static Room get(String topic, int appId) {
        return Room.get(topic, appId);
    }

    // 获得主题数据刷新数据
    public static boolean getUpdateStatus(Room room) {
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

    public static void _onChanged(Room room) {
        /*
        if (distribution_subscribe != null) {
            if (distribution_subscribe.pushStatus(room) == null) {
                nLogger.errorInfo("分布式订阅中间件故障，回退到本地默认订阅模式");
                distribution_subscribe = null;
            }
        }
        */
        room.fleshSyncUpdateTime();
    }
}
