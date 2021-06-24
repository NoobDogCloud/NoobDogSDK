package common.java.Rpc;

public record RpcLocation(String url) {

    public static RpcLocation Instant(String url) {
        return new RpcLocation(url);
    }

    public String toString() {
        return this.url;
    }
}
