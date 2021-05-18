package common.java.Concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LazyTask {
    private static final ConcurrentHashMap<Integer, LazyTask> TaskStore = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService service = Executors
            .newSingleThreadScheduledExecutor();

    static {
        service.scheduleAtFixedRate(() -> {
            long currentSeconds = System.currentTimeMillis() / 1000;
            TaskStore.values().forEach(lt -> {
                if (currentSeconds - lt.lastTime() >= lt.delay()) {
                    lt.runImp();
                }
            });
        }, 1, 1, TimeUnit.SECONDS);

    }

    private final long delay;// s
    private final int maxCnt;
    private final Runnable task;
    private long updateTime = 0;
    private int cnt = 0;

    private LazyTask(Runnable task, int maxCnt, long delay) {
        this.task = task;
        this.maxCnt = maxCnt;
        this.delay = delay;
    }

    public static LazyTask build(Runnable task) {
        return build(task, 500, 5);
    }

    public static LazyTask build(Runnable task, int maxCnt) {
        return build(task, maxCnt, 5);
    }

    public static LazyTask build(Runnable task, int maxCnt, long seconds) {
        LazyTask lt = new LazyTask(task, maxCnt, seconds);
        LazyTask.TaskStore.put(lt.hashCode(), lt);
        return lt;
    }

    public void updateTime() {
        updateTime = System.currentTimeMillis() / 1000;
    }

    public long delay() {
        return delay;
    }

    public long lastTime() {
        return updateTime;
    }

    private void release() {
        LazyTask.TaskStore.remove(this.hashCode());
    }

    public void run() {
        cnt++;
        if (cnt >= maxCnt) {
            runImp();
        } else {
            updateTime();
        }
    }

    public void runImp() {
        // 更新时间
        updateTime();
        // 计次清0
        cnt = 0;
        // 立刻运行
        task.run();
        release();
    }
}
