package common.java.DataSource.Subscribe;

import common.java.Cache.CacheHelper;
import common.java.Number.NumberHelper;

/**
 * 一个默认的分布式订阅类
 * 基于 redis 的分布式订阅(一个第三方注入的订阅服务示例)
 */
public class DistributionSubscribe implements DistributionSubscribeInterface {
    private static CacheHelper ca;
    private long updateLevel = 0;   // 数据是否更新权值

    private CacheHelper getCa(String appId) {
        if (ca == null) {
            ca = CacheHelper.buildByAppId(appId);
        }
        return ca;
    }

    // 推送 主题更新时间
    public Boolean pushStatus(Room room) {
        try {
            String topic = getDistributionKey(room.getTopicWithAppID());
            CacheHelper ca = getCa(room.getAppId());
            ca.set(topic, 0L);
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
            CacheHelper ca = getCa(room.getAppId());
            var v = ca.get(topic);
            if (v == null) {
                ca.set(topic, 0L);
                v = 0L;
            }
            long c = NumberHelper.number2long(v);
            if (c > 0x7FFFFFFF) {
                c = 0;
            }
            ca.set(topic, c + 1);
            ca.setExpire(topic, 86400 * 1000);
            return true;
        } catch (Exception e) {
            return null;
        }
    }

    // 获得是否更新状态
    public Boolean pullStatus(Room room) {
        try {
            // 如果当前权值小于新权值，则更新
            long _updateLevel = updateLevel;
            updateLevel = getCa(room.getAppId()).getLong(getDistributionKey(room.getTopicWithAppID()));
            return updateLevel > _updateLevel;
        } catch (Exception e) {
            return null;
        }
    }

    // 删除状态
    public void removeStatus(Room room) {
        getCa(room.getAppId()).delete(getDistributionKey(room.getTopicWithAppID()));
    }
}
