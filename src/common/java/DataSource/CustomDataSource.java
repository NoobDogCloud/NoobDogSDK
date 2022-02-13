package common.java.DataSource;

import common.java.DataSource.DataSourceStore.DataSourceManager;
import common.java.DataSource.DataSourceStore.IDataSourceStore;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @classNote: 自定义数据源 服务端通过本类生成新的数据源，并可以向数据源写入数据
 */
public class CustomDataSource {
    private final AtomicLong idx = new AtomicLong(0);
    private final IDataSourceStore store;
    private final String topic;

    /**
     * @apiNote 创建自定义数据源
     */
    private CustomDataSource() {
        topic = createTopic();
        store = DataSourceManager.add(topic);
    }

    public static CustomDataSource build() {
        return new CustomDataSource();
    }

    private String createTopic() {
        var ctx = HttpContext.current();
        String appId = StringHelper.toString(ctx == null ? 0 : ctx.appId());
        String key;
        do {
            // key = "CustomDataSource_" + StringHelper.shortUUID() + idx.incrementAndGet() + "_" + appId;
            key = "CustomDataSource_" + StringHelper.shortUUID() + idx.incrementAndGet();
            if (!DataSourceManager.contains(key)) {
                break;
            }
        } while (true);
        return key;
    }

    /**
     * @apiNote 销毁自定义数据源
     */
    public void delete() {
        DataSourceManager.remove(topic);
    }

    public String getTopic() {
        return topic;
    }

    // 向数据源写入数据
    public void add(Object data) {
        store.add(data);
    }
}
