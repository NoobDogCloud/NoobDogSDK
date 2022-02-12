package common.java.DataSource;

import common.java.DataSource.DataSourceStore.DataSourceManagerLocal;
import common.java.DataSource.DataSourceStore.DataSourceReader;
import common.java.DataSource.DataSourceStore.IDataSourceManager;
import common.java.DataSource.DataSourceStore.IDataSourceStore;
import common.java.DataSource.Subscribe.Room;
import common.java.Http.Server.ApiSubscribe.SubscribeGsc;
import common.java.Http.Server.HttpContext;
import io.netty.channel.ChannelId;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 从服务端动态创建订阅数据源
 */
public class CustomDataSource<T> {
    // 自定义数据源列表

    // 数据源房间
    private final Room room;
    // 成员数据读取水位管理
    private final ConcurrentHashMap<ChannelId, DataSourceReader> memberReaderMap = new ConcurrentHashMap<>();
    // 数据源管理器
    // private dataSourceManager;
    // 数据源存储
    private IDataSourceStore<T> dataSource;

    // 创建/获得一个 自定义数据源
    private CustomDataSource(String topic, IDataSourceManager<T> dataSourceManager) {
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
                });
        // 从数据源管理器获得数据源
        IDataSourceManager<T> _dataSourceManager = dataSourceManager == null ?
                DataSourceManagerLocal.build() :
                dataSourceManager;
        // 从数据源管理器获得数据源
        if (_dataSourceManager.contains(topic)) {
            dataSource = _dataSourceManager.get(topic);
        }
    }

    public static <T> CustomDataSource<T> build(String topic) {
        return new CustomDataSource<>(topic, null);
    }

    public static <T> CustomDataSource<T> build(String topic, IDataSourceManager<T> dataSourceManager) {
        return new CustomDataSource<>(topic, dataSourceManager);
    }

    // 使用自定义数据源
    public CustomDataSource injectDataSource(IDataSourceStore<T> dataSource) {
        if (dataSource != null) {
            this.dataSource = dataSource;
        }
        return this;
    }

    // 向数据源写入数据
    public void add(T data) {
        if (dataSource != null) {
            dataSource.add(data);
        }
        // 标记有新数据，记录数据更新时间
        room.fleshUpdateStatus().fleshSyncUpdateTime();
    }
}
