package common.java.Concurrency;

import common.java.Apps.AppContext;
import common.java.Interrupt.CacheAuth;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步任务支持类
 * 带入runner,返回QueryCode,任务完成30Day内获得任务状态，否则过期
 * 支持任务进度结构，提供基于QueryCode的进度查询
 */
public class AsyncTask {
    private static final ExecutorService service = Executors.newCachedThreadPool();

    private final AsyncTaskRunnable task;

    private AsyncTask(AsyncTaskRunnable task, long time_out) {
        this.task = task;
    }

    public static AsyncTask build(AsyncTaskRunnable task) {
        return build(task, 0);
    }

    public static AsyncTask build(AsyncTaskRunnable task, long time_out) {
        return new AsyncTask(task, time_out);
    }

    /**
     * 启动异步任务
     */
    public String run() {
        String queryKey = CacheAuth.build().getUniqueKey("async_thread_task", "{}");
        AsyncStruct aStruct = AsyncStruct.init(queryKey);
        AppContext.current().thread(() -> (task.run(aStruct) ? aStruct.success() : aStruct.fail()).save());
        return queryKey;
    }
}
