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
    private final int status;
    // 变化日志
    private final AsyncProgressInfo[] logs;
    // 当前进度
    private int position;
    // 开启日志时间
    private boolean enableTimestamp;

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

    public AsyncProgress addLog(String log) {
        if (position < logs.length) {
            logs[position] = AsyncProgressInfo.build(log, enableTimestamp);
            source.add(toString());
            position++;
        }
        return this;
    }

    public String toString() {
        return JSONObject.build()
                .put("progress", JSONObject.build()
                        .put("position", position)
                        .put("total", total))
                .put("status", status)
                .put("logs", logs[position].toString()).toString();
    }
}
