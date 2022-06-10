package common.java.DataSource;

import common.java.Concurrency.HashmapTaskRunner;
import common.java.DataSource.DataSourceStore.DataSourceManager;
import common.java.DataSource.DataSourceStore.DataSourceReader;
import common.java.DataSource.DataSourceStore.IDataSourceStore;
import common.java.DataSource.Subscribe.Room;
import common.java.Http.Server.ApiSubscribe.GscSubscribe;
import common.java.Http.Server.HttpContext;
import common.java.Rpc.rMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从服务端动态创建订阅数据源(订阅者)
 */
public class CustomDataSourceSubscriber {
    // 所有本节点订阅数据房间
    private static final HashmapTaskRunner<String, CustomDataSourceSubscriber> subscriber = HashmapTaskRunner.<String, CustomDataSourceSubscriber>getInstance((k, v) -> {
        if (v.isUpdate()) {
            v.freshUpdateStatus();
        }
    }).setDelay(150);

    // 数据源房间
    private final Room room;
    // 成员数据读取水位管理
    private final ConcurrentHashMap<ChannelId, DataSourceReader> memberReaderMap = new ConcurrentHashMap<>();
    // 数据源存储
    private IDataSourceStore dataSource;
    // 以广播数据行数
    private long sendNumber = 0;

    // 创建/获得一个 自定义数据源
    private CustomDataSourceSubscriber(String topic, String appId, IDataSourceStore _dataSource) {
        room = GscSubscribe.updateOrCreate(topic, appId)
                // 设置数据广播方法
                .updateRefreshFunc(member -> {
                    // 更新数据源
                    var reader = memberReaderMap.get(member.getCh().channel().id());
                    if (reader == null) {
                        return;
                    }
                    // 从上次未读水位开始读取
                    List<Object> lines = dataSource.news(reader.getLastUnreadWater());
                    // 更新未读水位
                    try {
                        reader.setLastUnreadWater(dataSource.size());
                    } catch (Exception e) {
                        System.out.println(e);
                    }

                    // 发送数据
                    if (!lines.isEmpty()) {
                        member.send(topic, rMsg.netMSG(lines));
                    }
                })
                .setJoinHook(member -> {
                    // 创建读取水位管理
                    memberReaderMap.put(member.getCh().channel().id(), DataSourceReader.build());
                })
                .setLeaveHook(member -> {
                    // 删除读取水位管理
                    memberReaderMap.remove(member.getCh().channel().id());
                })
                .setBroadcastHook(room -> lockUpdateStatus());
        // 从数据源管理器获得数据源
        dataSource = _dataSource; // DataSourceManager.get(topic);
        subscriber.put(room.getTopicWithAppID(), this);
    }

    public static CustomDataSourceSubscriber build(String topic) {
        var ctx = HttpContext.current();
        if (ctx == null) {
            throw new RuntimeException("需要通过Api触发，不可以直接调用");
        }
        var appId = ctx.appId();
        String topicWithAppid = topic + "_" + appId;
        if (!subscriber.containsKey(topicWithAppid)) {
            var ds = DataSourceManager.get(topic, appId);
            if (ds != null) {
                new CustomDataSourceSubscriber(topic, appId, ds);
            }
        }
        return subscriber.get(topicWithAppid);
    }

    // 默认返回空
    public String result() {
        return "";
    }

    // 取消数据源
    public static void cancel(ChannelHandlerContext ch) {
        Room.removeMember(ch.channel().id());
    }

    public List<Object> getAllData() {
        return dataSource == null ? new ArrayList<>() : dataSource.all();

    }

    private CustomDataSourceSubscriber freshUpdateStatus() {
        GscSubscribe.update(room);
        return this;
    }

    private CustomDataSourceSubscriber lockUpdateStatus() {
        // 广播前，设置更新状态为否，表示未更新
        if (dataSource != null) {
            sendNumber = dataSource.size();
        }
        return this;
    }

    // 删除本节点订阅对象
    private void remove() {
        var r = GscSubscribe.get(room.getTopic(), room.getAppId());
        if (r != null) {
            // 删除订阅源，删除房间
            GscSubscribe.remove(r);
        }
        var _topic = room.getTopicWithAppID();
        if (_topic != null) {
            subscriber.remove(_topic);
        }
        memberReaderMap.clear();
    }

    // 判断数据源是否已更新
    public boolean isUpdate() {
        // 如果数据源为空或者异常主动删除本地对应订阅对象
        if (dataSource == null || dataSource.isInvalid()) {
            this.remove();
            return false;
        }
        return dataSource.size() > sendNumber;
    }
}
