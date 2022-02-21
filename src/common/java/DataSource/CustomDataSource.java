package common.java.DataSource;

import common.java.Concurrency.ListTaskRunner;
import common.java.DataSource.DataSourceStore.DataSourceManager;
import common.java.DataSource.DataSourceStore.IDataSourceStore;
import common.java.Http.Server.ApiSubscribe.GscSubscribe;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @classNote: 自定义数据源 服务端通过本类生成新的数据源，并可以向数据源写入数据
 */
public class CustomDataSource {
    // 自动强制释放间隔
    private static final long freeTimeout = 3600 * 1000;
    private final int appId;    // 所有待删除的自定义数据源
    private static final ListTaskRunner<CustomDataSource> waitDeleteQueue = ListTaskRunner.<CustomDataSource>getInstance(cds -> {
        var r = GscSubscribe.updateOrCreate(cds.topic, cds.appId);
        if (r == null) {
            cds._delete();
            CustomDataSource.removeDeleteTask(cds);
        }
    }).setDelay(1500);
    private long lastLiveTime;    // 所有当前进程的自定义数据源
    private static final ListTaskRunner<CustomDataSource> customDataSourceQueue = ListTaskRunner.<CustomDataSource>getInstance(cds -> {
        if ((TimeHelper.getNowTimestampByZero() - cds.lastLiveTime) > freeTimeout) {
            cds.delete();
        }
        // 1min 扫描一次
    }).setDelay(1000 * 60);

    private final AtomicLong idx = new AtomicLong(0);
    private final IDataSourceStore store;
    private final String topic;

    /**
     * @apiNote 创建自定义数据源
     */
    private CustomDataSource() {
        topic = createTopic();
        store = DataSourceManager.add(topic);
        appId = HttpContext.current().appId();
        updateLiveTime();
        customDataSourceQueue.add(this);
    }

    private static void removeDeleteTask(CustomDataSource v) {
        waitDeleteQueue.remove(v);
    }

    private void updateLiveTime() {
        lastLiveTime = TimeHelper.getNowTimestampByZero();
    }

    /**
     * @apiNote 销毁自定义数据源, 执行后不会立刻删除，而是等数据源对应的订阅者下线后才会删除
     */
    public void delete() {
        // 添加自己到删除队列，如果队列对应的订阅房间不存在，则删除自己
        waitDeleteQueue.add(this);
        customDataSourceQueue.remove(this);
    }

    private void _delete() {
        // 删除主题
        DataSourceManager.remove(topic);
        // 关闭数据存储源
        store.close();
    }

    public static CustomDataSource build() {
        return new CustomDataSource();
    }

    private String createTopic() {
        String key;
        do {
            key = "CustomDataSource_" + StringHelper.shortUUID() + idx.incrementAndGet();
            if (!DataSourceManager.contains(key)) {
                break;
            }
        } while (true);
        return key;
    }

    // 向数据源写入数据
    public void add(Object data) {
        if (!store.isInvalid()) {
            store.add(data);
            updateLiveTime();
        }
    }


    public String getTopic() {
        return topic;
    }


}
