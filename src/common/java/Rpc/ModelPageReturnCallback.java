package common.java.Rpc;

// 返回的模型数据 RpcPageInfo 过滤器
@FunctionalInterface
public interface ModelPageReturnCallback {
    RpcPageInfo run(RpcPageInfo returnValue);
}
