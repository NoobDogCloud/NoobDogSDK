package common.java.Http.Server.Subscribe;

import common.java.Cache.CacheHelper;

public class DistributionSubscribe implements DistributionSubscribeInterface {
    private final CacheHelper ca = CacheHelper.build();

    // 推送 主题更新时间
    public void pushStatus(String topic) {
        String _topic = getDistributionKey(topic);
        ca.set(_topic, true);
        ca.setExpire(_topic, 86400 * 1000);
    }

    // 拉取 主题更新状态
    public boolean pullStatus(String topic) {
        return ca.getBoolean(getDistributionKey(topic));
    }
}
