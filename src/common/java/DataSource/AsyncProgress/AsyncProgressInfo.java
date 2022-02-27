package common.java.DataSource.AsyncProgress;

import common.java.Time.TimeHelper;

public class AsyncProgressInfo {
    private final boolean withTimestamp;
    private final long timestampZero = TimeHelper.getNowTimestampByZero();
    private final String info;

    private AsyncProgressInfo(String info, boolean withTimestamp) {
        this.info = info;
        this.withTimestamp = withTimestamp;
    }

    public static AsyncProgressInfo build(String info) {
        return build(info, false);
    }

    public static AsyncProgressInfo build(String info, boolean withTimestamp) {
        return new AsyncProgressInfo(info, withTimestamp);
    }

    public String getInfo() {
        return info;
    }

    public long getTimestamp() {
        return timestampZero;
    }

    public String toString() {
        return (withTimestamp ? (timestampZero + ":") : "") + info;
    }
}
