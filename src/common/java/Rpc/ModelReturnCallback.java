package common.java.Rpc;

// 返回的模型数据 Object 过滤器
@FunctionalInterface
public interface ModelReturnCallback {
    Object run(Object returnValue);
}
