package common.java.Concurrency;

import common.java.Thread.ThreadHelper;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class HashmapTaskRunner<K, V> {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    List<HashMap<K, V>> lists;          // 总任务列表
    long maxWaterMark;                  // 每个队列最大数量
    BiConsumer<K, V> func;          // 为每个元素运行的任务
    // public static Object instance;
    long delayValue = 1000;

    private HashmapTaskRunner(long maxWaterMark, BiConsumer<K, V> func) {
        this.lists = new ArrayList<>(1);
        this.maxWaterMark = maxWaterMark;
        this.func = func;
        buildNew();
    }

    public static <K, V> HashmapTaskRunner<K, V> getInstance(long maxWaterMark, BiConsumer<K, V> func) {
        return new HashmapTaskRunner<>(maxWaterMark, func);
    }

    public static <K, V> HashmapTaskRunner<K, V> getInstance(BiConsumer<K, V> func) {
        return getInstance(5000, func);
    }

    public HashmapTaskRunner<K, V> setDelay(long delayValue) {
        this.delayValue = delayValue;
        return this;
    }

    private HashMap<K, V> buildNew() {
        return buildNew(null, null);
    }

    private HashMap<K, V> buildNew(K k, V v) {
        HashMap<K, V> newHashmap = new HashMap<>();
        if (k != null) {
            newHashmap.put(k, v);
        }
        lists.add(newHashmap);
        executorService.submit(() -> {
            do {
                newHashmap.forEach(func);
                if (lists.size() > 1 && newHashmap.size() == 0) {
                    lists.remove(newHashmap);
                }
                ThreadHelper.sleep(delayValue);
            } while (lists.size() > 0);
        });
        return newHashmap;
    }

    public void put(K k, V v) {
        for (HashMap<K, V> hashMap : lists) {
            if (hashMap.containsKey(k)) {
                hashMap.put(k, v);
                return;
            }
        }
        // 所有hashmap都没有key,找一个有位置的插入
        for (HashMap<K, V> hashMap : lists) {
            if (hashMap.size() < maxWaterMark) {
                hashMap.put(k, v);
                return;
            }
        }
        // 所有hashmap都满了,新建一个
        buildNew(k, v);
    }

    public void remove(K k) {
        for (HashMap<K, V> hashMap : lists) {
            if (hashMap.containsKey(k)) {
                hashMap.remove(k);
                return;
            }
        }
    }

    public boolean containsKey(K k) {
        for (HashMap<K, V> hashMap : lists) {
            if (hashMap.containsKey(k)) {
                return true;
            }
        }
        return false;
    }

    public V get(K k) {
        for (HashMap<K, V> hashMap : lists) {
            if (hashMap.containsKey(k)) {
                return hashMap.get(k);
            }
        }
        return null;
    }

    public void clear() {
        for (HashMap<K, V> hashMap : lists) {
            hashMap.clear();
        }
    }

    public Set<K> keys() {
        Set<K> setKeys = new HashSet<>();
        for (HashMap<K, V> hashMap : lists) {
            setKeys.addAll(hashMap.keySet());
        }
        return setKeys;
    }

    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        for (HashMap<K, V> hashMap : lists) {
            values.addAll(hashMap.values());
        }
        return values;
    }

    public int size() {
        int size = 0;
        for (HashMap<K, V> hashMap : lists) {
            size += hashMap.size();
        }
        return size;
    }

    public void close() {
        clear();
        lists.clear();
        executorService.shutdown();
    }
}
