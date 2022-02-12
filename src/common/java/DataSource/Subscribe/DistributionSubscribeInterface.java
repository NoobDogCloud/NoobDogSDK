package common.java.DataSource.Subscribe;

public interface DistributionSubscribeInterface {
    String prefix = "GSC_DistributionSubscribe_";

    // 创建和推送状态 不需要激发
    Boolean pushStatus(Room room);

    // 获得状态
    Boolean pullStatus(Room room);

    // 设置状态需要激发
    Boolean fleshStatus(Room room);

    // 获得分布式时间KEY
    default String getDistributionKey(String p) {
        return prefix + p;
    }
}
