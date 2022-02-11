package common.java.Http.Server.Subscribe;

import common.java.Cache.CacheHelper;

/**
 * 一个默认的分布式订阅类
 * 基于 redis 的分布式订阅(一个第三方注入的订阅服务示例)
 */
public class DistributionSubscribe implements DistributionSubscribeInterface {
    private final CacheHelper ca = CacheHelper.build();

    // 推送 主题更新时间
    public Boolean pushStatus(String topic) {
        String _topic = getDistributionKey(topic);
        try {
            ca.set(_topic, true);
            ca.setExpire(_topic, 86400 * 1000);
            return true;
        } catch (Exception e) {
            return null;
        }
    }

    // 拉取 主题更新状态
    public Boolean pullStatus(String topic) {
        try {
            return ca.getBoolean(getDistributionKey(topic));
        } catch (Exception e) {
            return null;
        }
    }
}
