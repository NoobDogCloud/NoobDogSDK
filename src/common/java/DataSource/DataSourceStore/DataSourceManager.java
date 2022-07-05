package common.java.DataSource.DataSourceStore;

import java.util.concurrent.ConcurrentHashMap;

public class DataSourceManager {
    private final static ConcurrentHashMap<String, IDataSourceStore> topicQueue = new ConcurrentHashMap<>();
    private static IDataSourceStore dsc = DataSourceStoreLocal.build();

    // 根据数据源名称获得数据源
    public static IDataSourceStore get(String topic, String appId) {
        var _topic = getPrivateTopic(topic, appId);
        return topicQueue.get(_topic);
    }

    // 获得真实topic
    private static String getPrivateTopic(String topic, String appId) {
        return topic + "_" + appId;
    }

    /**
     * @param topic 数据源名称
     * @return 数据源对象
     * 为topic添加数据源
     */
    public static IDataSourceStore add(String topic, String appId) {
        var ds = dsc.newInstance();
        var _topic = getPrivateTopic(topic, appId);
        topicQueue.put(_topic, ds);
        return ds;
    }

    // 切换默认数据源存储管理器
    public static void injectDateSourceStore(IDataSourceStore dsc) {
        DataSourceManager.dsc = dsc;
    }

    public static boolean contains(String topic, String appId) {
        var _topic = getPrivateTopic(topic, appId);
        return topicQueue.containsKey(_topic);
    }

    public static void remove(String topic, String appId) {
        String topicFull = getPrivateTopic(topic, appId);
        topicQueue.remove(topicFull);
    }
}
