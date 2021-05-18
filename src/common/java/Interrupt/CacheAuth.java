package common.java.Interrupt;

import common.java.Cache.CacheHelper;
import common.java.String.StringHelper;

public class CacheAuth {
    private final CacheHelper cache = CacheHelper.build();//使用当前服务的缓存

    public static CacheAuth build() {
        return new CacheAuth();
    }

    public String getUniqueKey(String perfix_ssid, String v) {
        String k;
        do {
            k = perfix_ssid + "_" + StringHelper.numUUID();
        } while (cache.get(k) != null);
        cache.set(k, v);
        return k;
    }

    public boolean breakRun(String ssid, String code) {
        cache.getSet(ssid, 30, code);
        return true;
    }

    public boolean breakRun(String ssid, String code, int expire) {
        cache.getSet(ssid, expire, code);
        return true;
    }

    public boolean resumeRun(String ssid, String code) {
        Object rString = cache.get(ssid);
        boolean rb = rString != null && rString.equals(code);
        if (rb) {
            cache.delete(ssid);
        }
        return rb;
    }
}
