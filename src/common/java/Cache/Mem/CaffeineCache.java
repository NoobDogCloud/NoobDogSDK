package common.java.Cache.Mem;

import com.github.benmanes.caffeine.cache.Caffeine;
import common.java.Cache.Common.InterfaceCache;
import common.java.Number.NumberHelper;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * 增加单条过期时间特性
 */
public class CaffeineCache implements InterfaceCache {
    private static final com.github.benmanes.caffeine.cache.Cache<String, Object> static_jdc;

    static {
        static_jdc = Caffeine.newBuilder()
                .maximumSize(500000)
                .initialCapacity(500)
                // .refreshAfterWrite(5, TimeUnit.SECONDS)
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .build();
    }

    private final com.github.benmanes.caffeine.cache.Cache<String, Object> jdc;

    private CaffeineCache() {
        jdc = static_jdc;
    }

    private CaffeineCache(long second) {
        jdc = buildCaffeine(second);
    }

    private static com.github.benmanes.caffeine.cache.Cache<String, Object> buildCaffeine(long second) {
        return Caffeine.newBuilder()
                .maximumSize(500000)
                .initialCapacity(500)
                // .refreshAfterWrite(5, TimeUnit.SECONDS)
                .expireAfterWrite(second, TimeUnit.SECONDS)
                .build();
    }

    public static CaffeineCache build(long second) {
        return new CaffeineCache(second);
    }

    public static CaffeineCache build() {
        return build(86400);
    }

    public static CaffeineCache getInstance() {
        return new CaffeineCache();
    }

    private boolean isExpire(long expireAt) {
        var n = TimeHelper.getNowTimestampByZero();
        return n > expireAt;
    }

    private JSONObject _getJson(Object v) {
        return (v instanceof JSONObject) ? (JSONObject) v : JSONObject.build(v.toString());
    }

    public Object get(String objectName) {
        var v = jdc.getIfPresent(objectName);
        if (v == null) {
            return null;
        }
        JSONObject r = _getJson(v);
        if (JSONObject.isInvalided(r)) {
            return null;
        }
        if (isExpire(r.getLong("_expireAt"))) {
            delete(objectName);
            return null;
        }
        return r.get("_store");
    }

    public int getInt(String objectName) {
        return NumberHelper.number2int(get(objectName));
    }

    public long getLong(String objectName) {
        return NumberHelper.number2long(get(objectName));
    }

    public float getFloat(String objectName) {
        return NumberHelper.number2float(get(objectName));
    }

    public double getDouble(String objectName) {
        return NumberHelper.number2double(get(objectName));
    }

    public boolean getBoolean(String objectName) {
        return Boolean.parseBoolean(StringHelper.toString(get(objectName)));
    }

    public String getString(String objectName) {
        return StringHelper.toString(get(objectName));
    }

    public BigDecimal getBigDecimal(String objectName) {
        return BigDecimal.valueOf(getLong(objectName));
    }

    public BigInteger getBigInteger(String objectName) {
        return BigInteger.valueOf(getInt(objectName));
    }

    public JSONObject getJson(String objectName) {
        return JSONObject.toJSON(StringHelper.toString(get(objectName)));
    }

    public JSONArray getJsonArray(String objectName) {
        return JSONArray.toJSONArray(StringHelper.toString(get(objectName)));
    }

    /**
     * @param objectName
     * @param expire     秒
     * @return
     */
    @Override
    public boolean setExpire(String objectName, int expire) {
        var v = get(objectName);
        if (v == null) {
            return false;
        }
        set(objectName, expire, v);
        return true;
    }

    @Override
    public String set(String objectName, Object objectValue) {
        return set(objectName, 86400, objectValue);
    }

    @Override
    public String set(String objectName, int expire, Object objectValue) {
        var now_time = TimeHelper.getNowTimestampByZero();
        var data = JSONObject.build("_createAt", now_time)
                .put("_expireAt", now_time + expire)
                .put("_store", objectValue);
        jdc.put(objectName, data);
        return "OK";
    }

    @Override
    public boolean setNX(String objectName, Object objectValue) {
        if (get(objectName) == null) {
            set(objectName, objectValue);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Object getSet(String objectName, Object objectValue) {
        Object rs = get(objectName);
        set(objectName, objectValue);
        return rs;
    }

    @Override
    public Object getSet(String objectName, int expire, Object objectValue) {
        Object rs = get(objectName);
        set(objectName, expire, objectValue);
        return rs;
    }

    @Override
    public long inc(String objectName) {
        long r = NumberHelper.number2long(get(objectName));
        long i = r + 1;
        set(objectName, i);
        return i;
    }

    @Override
    public long incBy(String objectName, long num) {
        long r = NumberHelper.number2long(get(objectName));
        long i = r + num;
        set(objectName, i);
        return i;
    }

    @Override
    public long dec(String objectName) {
        long r = NumberHelper.number2long(get(objectName));
        long i = r - 1;
        set(objectName, i);
        return i;
    }

    @Override
    public long decBy(String objectName, long num) {
        long r = NumberHelper.number2long(get(objectName));
        long i = r - num;
        set(objectName, i);
        return i;
    }

    @Override
    public long delete(String objectName) {
        jdc.invalidate(objectName);
        return 1;
    }
}
