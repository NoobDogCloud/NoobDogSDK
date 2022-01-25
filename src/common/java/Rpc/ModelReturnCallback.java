package common.java.Rpc;

@FunctionalInterface
public interface ModelReturnCallback {
    Object run(Object returnValue);
}
