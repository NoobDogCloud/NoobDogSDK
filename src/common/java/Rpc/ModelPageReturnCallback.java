package common.java.Rpc;

@FunctionalInterface
public interface ModelPageReturnCallback {
    RpcPageInfo run(RpcPageInfo returnValue);
}
