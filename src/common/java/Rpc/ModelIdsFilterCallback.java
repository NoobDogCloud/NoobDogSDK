package common.java.Rpc;

// 被操作模型数据对象的 id 组
@FunctionalInterface
public interface ModelIdsFilterCallback {
    /**
     * @param ids 被操作对象的id组
     * @return
     */
    FilterReturn run(String[] ids);
}
