package common.java.Rpc;

@FunctionalInterface
public interface ReturnCallback {
    Object run(String funcName, Object[] parameter, Object returnValue);
    // void exec(String funcName,Object parameters, Object returnValue);
}
