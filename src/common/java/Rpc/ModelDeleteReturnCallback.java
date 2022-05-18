package common.java.Rpc;

@FunctionalInterface
public interface ModelDeleteReturnCallback {
    Object run(Object returnValue, String[] ids);
}
