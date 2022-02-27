package common.java.DataSource.AsyncProgress;

import common.java.DataSource.CustomDataSource;
import common.java.GscCommon.CheckModel;
import org.json.gsc.JSONObject;

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
    private final AsyncProgressInfo[] logs;
    // 当前进度
    private int position;
    // 开启日志时间
    private boolean enableTimestamp;
    // 当前最新任务块
    private AsyncProgressInfo currentInfo;

    private AsyncProgress(CustomDataSource source, int total) {
        this.source = source;
        this.total = total;
        this.position = 0;
        this.status = CheckModel.running;
        this.logs = new AsyncProgressInfo[total];
        this.enableTimestamp = false;
    }

    public static AsyncProgress build(CustomDataSource source, int total) {
        return new AsyncProgress(source, total);
    }

    public AsyncProgress setEnableTimestamp(boolean enableTimestamp) {
        this.enableTimestamp = enableTimestamp;
        return this;
    }

    private void updateInfo(String info) {
        currentInfo = AsyncProgressInfo.build(info, enableTimestamp);
    }

    public synchronized AsyncProgress addLog(String log) {
        if (position < logs.length) {
            updateInfo(log);
            logs[position] = currentInfo;
            position++;
            source.add(toString());
        }
        return this;
    }

    public AsyncProgress setStatus(int status) {
        this.status = status;
        return this;
    }

    public String toString() {
        var log = currentInfo;
        return JSONObject.build()
                .put("progress", JSONObject.build()
                        .put("position", position)
                        .put("total", total))
                .put("timestamp", log.getTimestamp())
                .put("status", status)
                .put("logs", log.getInfo()).toString();
    }
}
