package common.java.Rpc;

@FunctionalInterface
public interface ModelIdsFilterCallback {
    FilterReturn run(String[] ids);
}
