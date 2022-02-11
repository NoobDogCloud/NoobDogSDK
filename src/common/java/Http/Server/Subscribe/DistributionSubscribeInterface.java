package common.java.Http.Server.Subscribe;

public interface DistributionSubscribeInterface {
    String prefix = "GSC_DistributionSubscribe_";

    // 更新状态
    Boolean pushStatus(String topic);

    // 获得状态
    Boolean pullStatus(String topic);

    // 获得分布式时间KEY
    default String getDistributionKey(String p) {
        return prefix + p;
    }
}
