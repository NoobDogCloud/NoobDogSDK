package common.java.Rpc;

@FunctionalInterface
public interface FilterCallback {
    /**
     * @param funcName  函数名
     * @param parameter 参数
     * @return 过滤状态
     */
    FilterReturn run(String funcName, Object[] parameter);
}
