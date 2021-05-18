package common.java.Concurrency;

import common.java.Cache.CacheHelper;
import common.java.File.FileText;
import common.java.nLogger.nLogger;

public class DistributedLocker {
    private static final String logFileName = "GlobelLocker.log";
    private static final String lockerPrefix = "Grape520Locker_";

    static {
        // 本地JVM第一次载入类时，清除所有未释放全局锁，防止出现死锁
        CacheHelper ch = CacheHelper.build();
        FileText textFile = FileText.build(logFileName);
        textFile.read().forEach(lockerInfo -> {
            String[] locker = lockerInfo.split("\\|");
            if (Integer.parseInt(locker[1]) == distributedLockerMode.GlobalMode) {
                ch.Global(true);
            }
            ch.delete(locker[0]);
        });
        nLogger.debugInfo("清除全局锁完成...");
        textFile.delete();
    }

    private boolean globalMode;
    private CacheHelper ch;
    private String lockerName;

    public DistributedLocker(String lockerName) {
        init(lockerName, distributedLockerMode.AppMode);
    }

    public DistributedLocker(String lockerName, int mode) {
        init(lockerName, mode);
    }

    public static DistributedLocker newLocker(String lockerName) {
        return new DistributedLocker(lockerName);
    }

    public static DistributedLocker newLocker(String lockerName, int mode) {
        return new DistributedLocker(lockerName, mode);
    }

    private void init(String lockerName, int mode) {
        this.lockerName = lockerPrefix + lockerName;
        this.globalMode = mode == distributedLockerMode.GlobalMode;
        // 锁设置成功
        if (getRedis().setNX(lockerPrefix + lockerName, false)) {
            FileText.build(logFileName).appendLine(this.lockerName + "|" + mode);
        }
    }

    private CacheHelper getRedis() {
        if (ch == null) {
            ch = CacheHelper.build();
        }
        return ch.Global(this.globalMode);
    }

    /**
     * 返回true表示现在锁定中，否则未锁
     *
     * @return
     */
    public boolean lock() {
        String val = getRedis().getSet(lockerName, true).toString();
        boolean rb = Boolean.parseBoolean(val);
        return !rb;
    }

    /**
     * 是否已经锁上
     *
     * @return
     */
    public boolean islocked() {
        String val = getRedis().get(lockerName).toString();
        return Boolean.parseBoolean(val);
    }

    /**
     * 是否存在该锁
     *
     * @return
     */
    public boolean isExisting() {
        return getRedis().get(lockerName) != null;
    }

    /**
     * 返回true表示现在锁定中，否则未锁
     *
     * @return
     */
    public boolean unlock() {
        String val = getRedis().getSet(lockerName, false).toString();
        return Boolean.parseBoolean(val);
    }

    /**
     * 删除锁
     */
    public void releaseLocker() {
        getRedis().delete(lockerName);
    }

    public static class distributedLockerMode {
        public static final int AppMode = 0;
        public static final int GlobalMode = 1;
    }
}
