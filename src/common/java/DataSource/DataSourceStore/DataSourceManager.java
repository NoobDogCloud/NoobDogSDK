package common.java.DataSource.DataSourceStore;

import common.java.Http.Server.HttpContext;

import java.util.concurrent.ConcurrentHashMap;

public class DataSourceManager {
    private final static ConcurrentHashMap<String, IDataSourceStore> topicQueue = new ConcurrentHashMap<>();
    private static IDataSourceStore dsc = DataSourceStoreLocal.build();

    // 根据数据源名称获得数据源
    public static IDataSourceStore get(String topic) {
        var _topic = getPrivateTopic(topic);
        return _topic != null ? topicQueue.get(_topic) : null;
    }

    // 获得真实topic
    private static String getPrivateTopic(String topic) {
        var ctx = HttpContext.current();
        if (ctx == null)
            return null;
        return topic + "_" + ctx.appId();
    }

    /**
     * @param topic 数据源名称
     * @return 数据源对象
     * @apiNote 为topic添加数据源
     */
    public static IDataSourceStore add(String topic) {
        var ds = dsc.newInstance();
        var _topic = getPrivateTopic(topic);
        if (_topic != null) {
            topicQueue.put(_topic, ds);
        }
        return ds;
    }

    // 切换默认数据源存储管理器
    public static void injectDateSourceStore(IDataSourceStore dsc) {
        DataSourceManager.dsc = dsc;
    }

    public static boolean contains(String topic) {
        var _topic = getPrivateTopic(topic);
        return _topic != null && topicQueue.containsKey(_topic);
    }

    public static void remove(String topic) {
        String topicFull = getPrivateTopic(topic);
        if (topicFull != null) {
            topicQueue.remove(topicFull);
        }
    }
}
