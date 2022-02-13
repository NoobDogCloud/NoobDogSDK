package common.java.Cache;

import common.java.Cache.Common.InterfaceCache;
import common.java.Coordination.Coordination;
import common.java.Http.Server.HttpContext;
import common.java.Number.NumberHelper;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

public class CacheHelper implements InterfaceCache {
    private final Cache cache;
    private String appPrefix;

    private CacheHelper(String configName, boolean globalShare) {
        cache = Cache.getInstance(configName, globalShare);
        Global(false);
    }

    public static CacheHelper build() {
        return new CacheHelper(null, true);
    }

    public static CacheHelper build(String configName, boolean globalShare) {
        return new CacheHelper(configName, globalShare);
    }

    public static CacheHelper buildLocal() {
        return new CacheHelper(null, false);
    }

    public static CacheHelper build(boolean globalShare) {
        return new CacheHelper(null, globalShare);
    }

    public static CacheHelper build(String configName) {
        return new CacheHelper(configName, true);
    }

    public static CacheHelper build(int appId) {
        var ctx = Coordination.getInstance().getAppContext(appId);
        if (ctx == null) {
            nLogger.errorInfo("当前应用[" + appId + "]无效");
            return null;
        }
        var appCfg = ctx.config().cache();
        if (appCfg == null) {
            nLogger.errorInfo("当前应用[" + appId + "]没有配置缓存");
        }
        return CacheHelper.build(appCfg);
    }

    public static CacheHelper buildForApp() {
        return CacheHelper.build(HttpContext.current().appId());
    }

    public CacheHelper secondCache(boolean flag) {
        cache.secondCache(flag);
        return this;
    }

    public CacheHelper Global(boolean flag) {
        appPrefix = "";
        if (!flag) {
            HttpContext hCtx = HttpContext.current();
            if (hCtx != null) {
                int appID = hCtx.appId();
                appPrefix = appID > 0 ? "_" + appID : "";
            }
        }
        return this;
    }

    private String prefixHook(String key) {
        return key + appPrefix;
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键值
     * @return 缓存数据的类型（需转换类型）
     */
    public Object get(String key) {
        return cache.get(prefixHook(key));
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

    public JSONObject getJson(String key) {
        return cache.getJson(prefixHook(key));
    }

    public JSONArray getJsonArray(String key) {
        return cache.getJsonArray(prefixHook(key));
    }

    /**
     * 获取缓存数据
     *
     * @param key 缓存键值
     * @return 缓存数据的类型（需转换类型）
     */
    @Override
    public String set(String key, int expire, Object value) {
        return cache.set(prefixHook(key), expire, value);
    }

    @Override
    public String set(String key, Object value) {
        return cache.set(prefixHook(key), value);
    }

    @Override
    public boolean setExpire(String objectName, int expire) {
        return cache.setExpire(prefixHook(objectName), expire);
    }

    @Override
    public long dec(String objectName) {
        return cache.dec(prefixHook(objectName));
    }

    @Override
    public long decBy(String objectName, long num) {
        return cache.decBy(prefixHook(objectName), num);
    }

    @Override
    public long inc(String objectName) {
        return cache.inc(prefixHook(objectName));
    }

    @Override
    public long incBy(String objectName, long num) {
        return cache.incBy(prefixHook(objectName), num);
    }


    /**
     * 存入缓存数据
     *
     * @param key   缓存键值
     * @param value 缓存数据
     */
    public Object getSet(String key, Object value) {
        return cache.getSet(prefixHook(key), value);
    }

    /**
     * 存入缓存数据
     *
     * @param key   缓存键值
     * @param value 缓存数据
     */
    public Object getSet(String key, int expire, Object value) {
        return cache.getSet(prefixHook(key), expire, value);
    }

    /**
     * 存入缓存数据，
     *
     * @param key   缓存键值
     * @param value 缓存数据
     */
    public boolean setNX(String key, Object value) {
        return cache.setNX(prefixHook(key), value);
    }

    /**
     * 删除数据
     *
     * @param key 缓存键值
     */
    public long delete(String key) {
        return cache.delete(prefixHook(key));
    }

    /**
     * 为缓存系统生成唯一随机 key
     *
     * @param prefix 前缀
     */
    public String generateUniqueKey(String prefix) {
        String key;
        do {
            key = prefix + "_" + StringHelper.shortUUID();
        } while (cache.get(key) != null);
        return key;
    }

}
