package common.java.Rpc;

public class RpcLocation {
    private final String url;

    private RpcLocation(String url) {
        this.url = url;
    }

    public static RpcLocation Instant(String url) {
        return new RpcLocation(url);
    }

    public String toString() {
        return this.url;
    }
}
