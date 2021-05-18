package common.java.Cache.Mem;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import common.java.nLogger.nLogger;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MemCache<K, V> {
    /**
     * @description: 内存缓存。缓存加载之后永不过期，刷新失败时将继续返回旧缓存。
     * 在调用getValue之前，需要设置 refreshDuration， refreshTimeunit， maxSize 三个参数
     * @param <K>
     * @param <V>
     */
    // 缓存自动刷新周期
    protected int refreshDuration = 10;
    // 缓存刷新周期时间格式
    protected TimeUnit refreshTimeunit = TimeUnit.MINUTES;
    // 缓存过期时间（可选择）
    protected int expireDuration = -1;
    // 缓存刷新周期时间格式
    protected TimeUnit expireTimeunit = TimeUnit.HOURS;
    // 缓存最大容量
    protected int maxSize = 4;
    protected Function<K, V> _getValueWhenExpired;

    private LoadingCache<K, V> cache = null;

    public static <K, V> MemCache<K, V> buildMemCache() {
        return new MemCache<>();
    }

    // public abstract void loadValueWhenStarted();

    // protected abstract V getValueWhenExpired(K key) throws Exception;


    /**
     * @param key
     * @throws Exception
     * @description: 从cache中拿出数据操作
     * @author: luozhuo
     * @date: 2017年6月13日 下午5:07:11
     */
    public V getValue(K key) {
        try {
            return getCache().get(key);
        } catch (Exception e) {
            nLogger.errorInfo(e, "从内存缓存中获取内容时发生异常，key: " + key);
        }
        return null;
    }

    public V getValueOrDefault(K key, V defaultValue) {
        try {
            return getCache().get(key);
        } catch (Exception e) {
            nLogger.errorInfo(e, "从内存缓存中获取内容时发生异常，key: " + key);
            return defaultValue;
        }
    }

    /**
     * 设置基本属性
     */
    public MemCache<K, V> setRefreshDuration(int refreshDuration) {
        this.refreshDuration = refreshDuration;
        return this;
    }

    public MemCache<K, V> setRefreshTimeUnit(TimeUnit refreshTimeunit) {
        this.refreshTimeunit = refreshTimeunit;
        return this;
    }

    public MemCache<K, V> setExpireDuration(int expireDuration) {
        this.expireDuration = expireDuration;
        return this;
    }

    public MemCache<K, V> setExpireTimeUnit(TimeUnit expireTimeunit) {
        this.expireTimeunit = expireTimeunit;
        return this;
    }

    public MemCache<K, V> setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public MemCache<K, V> setGetValueWhenExpired(Function<K, V> func) {
        this._getValueWhenExpired = func;
        return this;
    }

    public void clearAll() {
        this.getCache().invalidateAll();
    }

    /**
     * @description: 获取cache实例
     */
    private LoadingCache<K, V> getCache() {
        if (cache == null) {
            Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                    .maximumSize(maxSize);

            if (refreshDuration > 0) {
                cacheBuilder.refreshAfterWrite(refreshDuration, refreshTimeunit);
            }
            if (expireDuration > 0) {
                cacheBuilder.expireAfterWrite(expireDuration, expireTimeunit);
            }

            cache = cacheBuilder.build(new CacheLoader<>() {
                @Override
                public V load(K key) {
                    return _getValueWhenExpired.apply(key);
                }

                @Override
                public V reload(final K key, V oldValue) {
                    return _getValueWhenExpired.apply(key);
                }
            });
        }
        return cache;
    }

    @Override
    public String toString() {
        return "GuavaCache";
    }
}
