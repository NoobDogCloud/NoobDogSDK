package common.java.Concurrency;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * 对象池对象
 */
public class ObjectPool<T> {
    private final ConcurrentLinkedQueue<T> pool;
    private final int maxSize;
    private final Supplier<T> producer;
    private int minSize;
    private final int blockSize = minSize;

    private ObjectPool(Supplier<T> fn, int maxSize, int minSize) {
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.producer = fn;
        pool = new ConcurrentLinkedQueue<>();
    }

    public static <F> ObjectPool<F> build(Supplier<F> fn) {
        return new ObjectPool<F>(fn, 10000, 10);
    }

    public static <F> ObjectPool<F> build(Supplier<F> fn, int maxSize, int minSize) {
        return new ObjectPool<F>(fn, maxSize, minSize);
    }

    private void injectPool() {
        for (int i = 0; i < blockSize; i++) {
            pool.add(producer.get());
        }
    }

    public T get() {
        if (pool.isEmpty()) {
            injectPool();
        }
        return pool.poll();
    }

    public void back(T t) {
        if (pool.size() < maxSize) {
            pool.add(t);
        }
    }
}
