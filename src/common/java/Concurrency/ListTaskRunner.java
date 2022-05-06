package common.java.Concurrency;

import common.java.Thread.ThreadHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 负责维护巨大队列，并管理他们的任务线程
 */
public class ListTaskRunner<T> {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    final List<List<T>> lists;    // 总任务列表
    final long maxWaterMark;      // 每个队列最大数量
    final Consumer<T> func;       // 为每个元素运行的任务
    long delayValue = 1000;

    private ListTaskRunner(long maxWaterMark, Consumer<T> func) {
        this.lists = new ArrayList<>(1);
        this.maxWaterMark = maxWaterMark;
        this.func = func;
        buildNew();
    }

    public static <T> ListTaskRunner<T> getInstance(long maxWaterMark, Consumer<T> func) {
        return new ListTaskRunner<>(maxWaterMark, func);
    }

    public static <T> ListTaskRunner<T> getInstance(Consumer<T> func) {
        return getInstance(5000, func);
    }

    public ListTaskRunner<T> setDelay(long delayValue) {
        this.delayValue = delayValue;
        return this;
    }

    private List<T> buildNew() {
        return buildNew(null);
    }

    private List<T> buildNew(T v) {
        List<T> newList = new ArrayList<>();
        if (v != null) {
            newList.add(v);
        }
        lists.add(newList);
        executorService.submit(() -> {
            do {
                newList.forEach(func);
                if (lists.size() > 1) {
                    lists.remove(newList);
                }
                ThreadHelper.sleep(50);
            } while (lists.size() > 0);
        });
        return newList;
    }

    public void add(T v) {
        for (List<T> list : lists) {
            if (list.size() < maxWaterMark) {
                list.add(v);
                return;
            }
        }
        // 没有空闲的队列，创建新的队列
        buildNew().add(v);
    }

    public void remove(T v) {
        for (List<T> list : lists) {
            if (list.remove(v)) {
                return;
            }
        }
    }

    public void clear() {
        for (List<T> list : lists) {
            list.clear();
        }
    }

    public int size() {
        int size = 0;
        for (List<T> list : lists) {
            size += list.size();
        }
        return size;
    }

    public void close() {
        clear();
        lists.clear();
        executorService.shutdown();
    }
}
