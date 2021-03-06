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
    private int total;
    // 当前状态
    private int status;
    // 变化日志
    private final List<ProgressInfo> logs;
    // 当前进度
    private int position;

    private AsyncProgress(CustomDataSource source, int total) {
        this.source = source;
        this.logs = new ArrayList<>();
        this.setTotal(total);
    }

    public static AsyncProgress build() {
        return new AsyncProgress(CustomDataSource.build(), 100);
    }

    public static AsyncProgress build(int total) {
        return new AsyncProgress(CustomDataSource.build(), total);
    }

    public String getTopic() {
        return source.getTopic();
    }

    public static AsyncProgress build(CustomDataSource source) {
        return new AsyncProgress(source, 100);
    }

    public static AsyncProgress build(CustomDataSource source, int total) {
        return new AsyncProgress(source, total);
    }

    public AsyncProgress setTotal(int total) {
        this.total = total;
        return this.reset();
    }

    public AsyncProgress reset() {
        this.position = 0;
        this.logs.clear();
        this.status = CheckModel.running;
        return this;
    }

    public synchronized AsyncProgress addLog(String log) {
        return addLog(log, 1);
    }

    public synchronized AsyncProgress addLog(String log, int stepValue) {
        return addLog(log, stepValue, ProgressInfo.InfoType.INFO);
    }

    public synchronized AsyncProgress addLog(String log, int stepValue, int type) {
        if (position < total) {
            logs.add(ProgressInfo.build(log, type));
            position += stepValue;
        }
        if (position == total) {
            status = CheckModel.success;
        }
        return this;
    }

    public synchronized AsyncProgress updateAndFlush() {
        String result = toString();
        if (result != null) {
            source.add(result);
        }
        return this;
    }

    public AsyncProgress setStatus(int status) {
        this.status = status;
        return this;
    }

    public String toString() {
        var progressInfo = logs.get(logs.size() - 1);
        // 进度块
        // 当前进度
        // 总进度
        // 时间戳
        // 状态
        // 类型
        // 日志
        return JSONObject.build()
                .put("progress", JSONObject.build()                     // 进度块
                        .put("position", position)                          // 当前进度
                        .put("total", total))                               // 总进度
                .put("timestamp", TimeHelper.getNowTimestampByZero())   // 时间戳
                .put("status", status)                                  // 状态
                .put("type", progressInfo.getType())                    // 类型
                .put("logs", progressInfo.getMessage()).toString();
    }

    public void close() {
        source.delete();
        logs.clear();
    }
}
