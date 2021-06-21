package common.java.Database;

public class SqlHelper {
    public static String func(String str) {
        return "func:" + str + " ";
    }

    /**
     * 10位unixtime
     */
    public static String now() {
        return SqlHelper.func("now()");
    }

    /**
     * 10位unixtime
     */
    public static String formUnixtime(long unixTime) {
        return func("from_unixtime(" + unixTime + ")");
    }
}
