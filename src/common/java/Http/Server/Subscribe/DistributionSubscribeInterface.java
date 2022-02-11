package common.java.Http.Server.Subscribe;

import common.java.Number.NumberHelper;

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

    // 获得分布式时间KEY
    default int getAppId(String topic) {
        var arr = topic.split("_");
        return NumberHelper.number2int(arr[arr.length - 1]);
    }
}
