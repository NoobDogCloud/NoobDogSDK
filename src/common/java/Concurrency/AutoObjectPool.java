package common.java.Concurrency;

/**
 * 对象池自动管理类
 */
public class AutoObjectPool<T> implements AutoCloseable {
    private final ObjectPool<T> pool;
    private T current;

    private AutoObjectPool(ObjectPool<T> pool) {
        this.pool = pool;
    }

    public static <T> AutoObjectPool<T> build(ObjectPool<T> pool) {
        return new AutoObjectPool<T>(pool);
    }

    /**
     * 获得对象
     */
    public T acquire() {
        current = pool.get();
        return current;
    }

    public void close() {
        if (current != null) {
            pool.back(current);
        }
    }
}
