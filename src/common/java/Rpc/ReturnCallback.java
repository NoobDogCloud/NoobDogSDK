package common.java.Rpc;

// 返回的过滤器
@FunctionalInterface
public interface ReturnCallback {
    /**
     * @param funcName    函数名
     * @param parameter   参数
     * @param returnValue 返回值
     * @return
     */
    Object run(String funcName, Object[] parameter, Object returnValue);
    // void exec(String funcName,Object parameters, Object returnValue);
}
