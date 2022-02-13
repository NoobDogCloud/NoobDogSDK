package common.java.DataSource.DataSourceStore;

import common.java.Http.Server.HttpContext;

import java.util.concurrent.ConcurrentHashMap;

public class DataSourceManager {
    private final static ConcurrentHashMap<String, IDataSourceStore> topicQueue = new ConcurrentHashMap<>();
    private static IDataSourceStore dsc = DataSourceStoreLocal.build();

    // 根据数据源名称获得数据源
    public static IDataSourceStore get(String topic) {
        return topicQueue.get(getPrivateTopic(topic));
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
        topicQueue.put(getPrivateTopic(topic), ds);
        return ds;
    }

    // 切换默认数据源存储管理器
    public static void injectDateSourceStore(IDataSourceStore dsc) {
        DataSourceManager.dsc = dsc;
    }

    public static boolean contains(String topic) {
        return topicQueue.containsKey(getPrivateTopic(topic));
    }

    public static void remove(String topic) {
        String topicFull = getPrivateTopic(topic);
        topicQueue.remove(topicFull);
    }
}
