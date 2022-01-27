package common.java.Rpc;

// 输入输出参数检查器组
@FunctionalInterface
public interface RpcJsonFilterCheckerCallback {
    FilterReturn run(RpcJsonFilterBlock block);
}
