package common.java.DataSource.DataSourceStore;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 hashmap 管理的数据源管理器
 */
public class DataSourceManagerLocal<T> implements IDataSourceManager<T> {
    private static DataSourceManagerLocal instance;
    private final ConcurrentHashMap<String, IDataSourceStore<T>> DataSourcePool = new ConcurrentHashMap<>();

    public static <T> DataSourceManagerLocal<T> build() {
        if (instance == null) {
            instance = new DataSourceManagerLocal<>();
        }
        return instance;
    }

    public IDataSourceStore<T> get(String topic) {
        if (DataSourcePool.containsKey(topic)) {
            return DataSourcePool.get(topic);
        }
        return null;
    }

    public void add(String topic, IDataSourceStore<T> dataSource) {
        DataSourcePool.put(topic, dataSource);
    }

    public void remove(String topic) {
        DataSourcePool.remove(topic);
    }

    public boolean contains(String topic) {
        return DataSourcePool.containsKey(topic);
    }
}
