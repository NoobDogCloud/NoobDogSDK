package common.java.DataSource;

import common.java.DataSource.DataSourceStore.DataSourceManager;
import common.java.DataSource.DataSourceStore.DataSourceReader;
import common.java.DataSource.DataSourceStore.IDataSourceStore;
import common.java.DataSource.Subscribe.Room;
import common.java.Http.Server.ApiSubscribe.SubscribeGsc;
import common.java.Http.Server.HttpContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 从服务端动态创建订阅数据源(订阅者)
 */
public class CustomDataSourceSubscriber {
    // 所有本节点订阅数据房间
    private static final ConcurrentHashMap<String, CustomDataSourceSubscriber> subscriber = new ConcurrentHashMap<>();
    // 定时检测数据源，如果数据源有更新，设置 room 状态为激发
    private static final ScheduledExecutorService heart_thread;

    static {
        heart_thread = Executors.newSingleThreadScheduledExecutor();
        heart_thread.scheduleAtFixedRate(() -> {
            for (CustomDataSourceSubscriber subscriber : subscriber.values()) {
                if (subscriber.isUpdate()) {
                    subscriber.freshUpdateStatus();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // 数据源房间
    private final Room room;
    // 成员数据读取水位管理
    private final ConcurrentHashMap<ChannelId, DataSourceReader> memberReaderMap = new ConcurrentHashMap<>();
    // 数据源存储
    private IDataSourceStore dataSource;
    // 以广播数据行数
    private long sendNumber = 0;

    // 创建/获得一个 自定义数据源
    private CustomDataSourceSubscriber(String topic) {
        var ctx = HttpContext.current();
        if (ctx == null) {
            throw new RuntimeException("需要通过Api触发，不可以直接调用");
        }
        room = SubscribeGsc.updateOrCreate(topic, ctx.appId())
                // 设置数据广播方法
                .updateRefreshFunc(member -> {
                    // 更新数据源
                    var reader = memberReaderMap.get(member.getCh().channel().id());
                    if (reader == null) {
                        return;
                    }
                    // 从上次未读水位开始读取
                    dataSource.news(reader.getLastUnreadWater());
                    // 更新未读水位
                    reader.setLastUnreadWater(dataSource.size());
                })
                .setJoinHook(member -> {
                    // 创建读取水位管理
                    memberReaderMap.put(member.getCh().channel().id(), DataSourceReader.build());
                })
                .setLeaveHook(member -> {
                    // 删除读取水位管理
                    memberReaderMap.remove(member.getCh().channel().id());
                })
                .setRoomDestroy(room -> {
                    // 删除房间
                    subscriber.remove(room.getTopic());
                    memberReaderMap.clear();
                })
                .setBroadcastHook(room -> lockUpdateStatus());
        // 从数据源管理器获得数据源
        dataSource = DataSourceManager.get(topic);
        subscriber.put(topic, this);
    }

    public static CustomDataSourceSubscriber build(String topic) {
        return new CustomDataSourceSubscriber(topic);
    }

    // 取消数据源
    public static void cancel(ChannelHandlerContext ch) {
        Room.removeMember(ch.channel().id());
    }

    public List<Object> getAllData() {
        return dataSource.all();
    }

    private CustomDataSourceSubscriber freshUpdateStatus() {
        room.fleshUpdateStatus().fleshSyncUpdateTime();
        return this;
    }

    private CustomDataSourceSubscriber lockUpdateStatus() {
        // 广播前，设置更新状态为否，表示未更新
        sendNumber = dataSource.size();
        return this;
    }

    // 判断数据源是否已更新
    public boolean isUpdate() {
        return dataSource.size() > sendNumber;
    }
}
