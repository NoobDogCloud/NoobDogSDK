package common.java.Rpc;

@FunctionalInterface
public interface FilterCallback {
    FilterReturn run(String funcName, Object[] parameter);
}
