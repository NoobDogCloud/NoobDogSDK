package common.java.DataSource.Subscribe;

import common.java.Cache.CacheHelper;
import common.java.Coordination.Coordination;
import common.java.nLogger.nLogger;

/**
 * 一个默认的分布式订阅类
 * 基于 redis 的分布式订阅(一个第三方注入的订阅服务示例)
 */
public class DistributionSubscribe implements DistributionSubscribeInterface {
    private static CacheHelper ca;

    private CacheHelper getCa(int appId) {
        if (ca == null) {
            var ctx = Coordination.getInstance().getAppContext(appId);
            if (ctx == null) {
                nLogger.errorInfo("当前应用[" + appId + "]无效");
                return null;
            }
            var appCfg = ctx.config().cache();
            if (appCfg == null) {
                nLogger.errorInfo("当前应用[" + appId + "]没有配置缓存");
            }
            ca = CacheHelper.build(appCfg);
        }
        return ca;
    }

    // 推送 主题更新时间
    public Boolean pushStatus(Room room) {
        try {
            String topic = getDistributionKey(room.getTopicWithAppID());
            CacheHelper ca = getCa(room.getAppId());
            ca.set(topic, false);
            ca.setExpire(topic, 86400 * 1000);
            return true;
        } catch (Exception e) {
            return null;
        }
    }

    // 设置 主题更新状态
    public Boolean fleshStatus(Room room) {
        try {
            String topic = getDistributionKey(room.getTopicWithAppID());
            getCa(room.getAppId()).set(topic, true);
            ca.setExpire(topic, 86400 * 1000);
            return true;
        } catch (Exception e) {
            return null;
        }
    }

    // 拉取 主题更新状态
    public Boolean pullStatus(Room room) {
        try {
            return getCa(room.getAppId()).getBoolean(getDistributionKey(room.getTopicWithAppID()));
        } catch (Exception e) {
            return null;
        }
    }
}
