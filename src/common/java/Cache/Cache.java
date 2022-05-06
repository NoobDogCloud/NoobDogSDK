package common.java.Cache;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Cache.Common.InterfaceCache;
import common.java.Cache.Mem.CaffeineCache;
import common.java.Cache.Redis.RedisCluster;
import common.java.Cache.Redis.RedisMasterSlave;
import common.java.Cache.Redis.RedisSentinel;
import common.java.Cache.Redis.RedisSingle;
import common.java.Config.Config;
import common.java.Number.NumberHelper;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;


public class Cache implements InterfaceCache {
    // 子缓存会用到该缓存对象
    public static final ConcurrentHashMap<String, Object> CacheClient;

    static {
        CacheClient = new ConcurrentHashMap<>();
    }

    private InterfaceCache _cache;                    // 共享缓存抽象对象
    private CaffeineCache _mem_cache;           // 内存缓存对象
    private boolean local_mem_cache = true;     // 开启二级缓存模式，默认开启

    /**
     * @param global_flag     全局共享模式，开启该状态后，系统可以跨机器实现缓存共享（基于第三方基础设施）,否则缓存仅限本进程
     * @param inputConfigName 缓存配置名称，单机模式下该参数无意义
     * @apiNote 初始化缓存对象
     */
    private Cache(boolean global_flag, String inputConfigName) {
        try {
            if (global_flag) {
                String configName = null;
                if (inputConfigName == null) {
                    if (MicroServiceContext.current().hasData()) {
                        configName = MicroServiceContext.current().config().cache();
                    } else if (AppContext.current().hasData()) {
                        configName = AppContext.current().config().cache();
                    }
                } else {
                    configName = inputConfigName;
                }
                // 获得共享缓存配置信息
                if (configName == null || configName.equals("")) {
                    nLogger.logInfo("缓存配置名称为空或者未配置");
                }
                _cache = getCacheObject(configName);
            } else {
                _cache = null;
            }
            // 获得内存缓存配置此案系
            _mem_cache = getMemCacheObject();
        } catch (Exception e) {
            // TODO: handle exception
            nLogger.logInfo(e, "缓存配置数据获取失败");
        }
    }

    public static Cache getInstance() {
        return new Cache(true, null);
    }

    public static Cache getInstance(String configName) {
        return new Cache(true, configName);
    }

    public static Cache getInstance(boolean global_share) {
        return new Cache(global_share, null);
    }

    public static Cache getInstance(String configName, boolean global_share) {
        return new Cache(global_share, configName);
    }

    /**
     * 控制本地二级缓存开关(没有共享缓存对象时，禁止关闭二级缓存)
     */
    public Cache secondCache(boolean flag) {
        if (_cache == null) {
            if (flag) {
                this.local_mem_cache = true;
            }
        } else {
            this.local_mem_cache = flag;
        }
        return this;
    }

    private CaffeineCache getMemCacheObject() {
        return CaffeineCache.getInstance();
    }

    private InterfaceCache getCacheObject(String cN) {
        String cacheName;
        JSONObject obj;
        String _configString = Config.netConfig(cN);
        try {
            if (_configString != null) {
                obj = JSONObject.toJSON(_configString);
                if (obj != null) {
                    cacheName = obj.getString("cacheName");
                    //缓存名
                    switch (cacheName) {
                        case "redis" -> {
                            _cache = new RedisSingle(_configString);
                        }
                        case "redis-cluster" -> {
                            _cache = new RedisCluster(_configString);
                        }
                        case "redis-sentinel" -> {
                            _cache = new RedisSentinel(_configString);
                        }
                        case "redis-masterslave" -> {
                            _cache = new RedisMasterSlave(_configString);
                        }
                        default -> _cache = CaffeineCache.build();
                    }
                } else {
                    nLogger.logInfo("Cache配置信息格式错误:" + _configString);
                }
            } else {
                nLogger.logInfo("cache配置信息[" + cN + "]为空:=>" + null);
            }
        } catch (Exception e) {
            nLogger.logInfo(e, "连接缓存系统失败! 配置名:[" + cN + "]");
            _cache = null;
        }
        return _cache;
    }

    private Object local_get(String objectName) {
        return this.local_mem_cache ? _mem_cache.get(objectName) : null;
    }

    public Object get(String objectName) {
        Object r = local_get(objectName);
        if (_cache != null && r == null) {
            r = _cache.get(objectName);
        }
        return r;
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
        Object r = local_get(objectName);
        if (_cache != null && r == null) {
            return _cache.getJson(objectName);
        }
        return JSONObject.toJSON(r.toString());
    }

    public JSONArray getJsonArray(String objectName) {
        Object r = local_get(objectName);
        if (_cache != null && r == null) {
            return _cache.getJsonArray(objectName);
        }
        return JSONArray.toJSONArray(r.toString());
    }

    public long inc(String objectName) {
        if (_cache != null) {
            long r = _cache.inc(objectName);
            if (local_mem_cache) {
                _mem_cache.set(objectName, r);
            }
            return r;
        } else {
            return local_mem_cache ? _mem_cache.inc(objectName) : 0;
        }
    }

    public long delete(String objectName) {
        if (local_mem_cache) {
            _mem_cache.delete(objectName);
        }
        if (_cache != null) {
            _cache.delete(objectName);
        }
        return 0;
    }

    public long dec(String objectName) {
        if (_cache != null) {
            long r = _cache.dec(objectName);
            if (local_mem_cache) {
                _mem_cache.set(objectName, r);
            }
            return r;
        } else {
            return local_mem_cache ? _mem_cache.dec(objectName) : 0;
        }
    }

    public long decBy(String objectName, long num) {
        if (_cache != null) {
            long r = _cache.decBy(objectName, num);
            if (local_mem_cache) {
                _mem_cache.set(objectName, r);
            }
            return r;
        } else {
            return local_mem_cache ? _mem_cache.decBy(objectName, num) : 0;
        }
    }

    public long incBy(String objectName, long num) {
        if (_cache != null) {
            long r = _cache.incBy(objectName, num);
            if (local_mem_cache) {
                _mem_cache.set(objectName, r);
            }
            return r;
        } else {
            return local_mem_cache ? _mem_cache.incBy(objectName, num) : 0;
        }
    }

    public boolean setExpire(String objectName, int expire) {
        boolean r = false;
        if (local_mem_cache) {
            r = _mem_cache.setExpire(objectName, expire);
        }
        if (_cache != null) {
            r = _cache.setExpire(objectName, expire);
        }
        return r;
    }

    public String set(String objectName, Object objectValue) {
        String r = null;
        if (local_mem_cache) {
            r = _mem_cache.set(objectName, objectValue);
        }
        if (_cache != null) {
            r = _cache.set(objectName, objectValue);
        }
        return r;
    }

    /**
     * @param objectName  缓存key
     * @param objectValue 缓存value
     * @param expire      缓存到期时间(秒)
     */
    public String set(String objectName, int expire, Object objectValue) {
        String r = null;
        if (local_mem_cache) {
            r = _mem_cache.set(objectName, expire, objectValue);
        }
        if (_cache != null) {
            r = _cache.set(objectName, expire, objectValue);
        }
        return r;
    }

    public Object getSet(String objectName, int expire, Object objectValue) {
        Object r = null;
        if (local_mem_cache) {
            r = _mem_cache.getSet(objectName, expire, objectValue);
        }
        if (_cache != null) {
            r = _cache.getSet(objectName, expire, objectValue);
        }
        return r;
    }

    public boolean setNX(String objectName, Object objectValue) {
        boolean r = false;
        if (local_mem_cache) {
            r = _mem_cache.setNX(objectName, objectValue);
        }
        if (_cache != null) {
            r = _cache.setNX(objectName, objectValue);
        }
        return r;
    }

    public Object getSet(String objectName, Object objectValue) {
        Object r = null;
        if (local_mem_cache) {
            r = _mem_cache.getSet(objectName, objectValue);
        }
        if (_cache != null) {
            r = _cache.getSet(objectName, objectValue);
        }
        return r;
    }
}