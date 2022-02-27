package common.java.DataSource.AsyncProgress;

import common.java.DataSource.CustomDataSource;
import common.java.GscCommon.CheckModel;
import common.java.Time.TimeHelper;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 异步方式反馈任务进度
 */
public class AsyncProgress {
    // 数据源
    private final CustomDataSource source;
    // 最大进度
    private final int total;
    // 当前状态
    private int status;
    // 变化日志
    private final List<String> logs;
    // 当前进度
    private int position;

    private AsyncProgress(CustomDataSource source, int total) {
        this.source = source;
        this.total = total;
        this.position = 0;
        this.status = CheckModel.running;
        this.logs = new ArrayList<>();
    }

    public static AsyncProgress build(CustomDataSource source, int total) {
        return new AsyncProgress(source, total);
    }

    public synchronized AsyncProgress addLog(String log) {
        return addLog(log, 1);
    }

    public synchronized AsyncProgress addLog(String log, int stepValue) {
        if (position < total) {
            logs.add(log);
            position += stepValue;
        }
        return this;
    }

    public synchronized void updateAndFlush() {
        String result = toString();
        if (result != null) {
            source.add(result);
        }
    }

    public AsyncProgress setStatus(int status) {
        this.status = status;
        return this;
    }

    public String toString() {
        return logs.size() > 0 ? JSONObject.build()
                .put("progress", JSONObject.build()
                        .put("position", position)
                        .put("total", total))
                .put("timestamp", TimeHelper.getNowTimestampByZero())
                .put("status", status)
                .put("logs", logs.get(logs.size() - 1)).toString() :
                null;
    }

    public void close() {
        source.delete();
        logs.clear();
    }
}
