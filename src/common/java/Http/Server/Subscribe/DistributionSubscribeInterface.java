package common.java.Http.Server.Subscribe;

public interface DistributionSubscribeInterface {
    String prefix = "GSC_DistributionSubscribe_";

    void pushStatus(String topic);

    boolean pullStatus(String topic);

    default String getDistributionKey(String p) {
        return prefix + p;
    }
}
