package common.java.Rpc;

@FunctionalInterface
public interface RpcJsonFilterCheckerCallback {
    FilterReturn run(RpcJsonFilterBlock block);
}
