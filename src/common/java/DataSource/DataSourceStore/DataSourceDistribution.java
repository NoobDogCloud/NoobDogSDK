package common.java.DataSource.DataSourceStore;

import common.java.Cache.CacheHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DataSourceDistribution implements IDataSourceStore {
    private CacheHelper ca;
    private String storeKey;
    private DataSourceTemplate dataSource;
    private Consumer<DataSourceTemplate> overflow_fn;
    private IGetOverflowResult getAll_fn;
    private int maxSize = 500;

    private DataSourceDistribution() {
        this.dataSource = DataSourceTemplate.build();
    }

    public static DataSourceDistribution build() {
        return new DataSourceDistribution();
    }

    private CacheHelper getCacheHelper() {
        if (ca == null) {
            ca = CacheHelper.buildForApp();
        }
        if (storeKey == null) {
            storeKey = ca.generateUniqueKey("custom_store_subscribe");
        }
        return ca;
    }

    private DataSourceTemplate updateStore() {
        getCacheHelper();
        var data = ca.getJson(storeKey);
        dataSource = data != null ? DataSourceTemplate.build(data) : DataSourceTemplate.build();
        return dataSource;
    }

    public boolean add(Object value) {
        try {
            // 缓存数据溢出时
            if (dataSource.size() >= maxSize) {
                // 溢出处理存在，执行溢出处理，并清空缓存
                if (overflow_fn != null) {
                    overflow_fn.accept(dataSource);
                    dataSource.clear();
                } else {
                    // 溢出处理不存在，删除第一个数据让位
                    dataSource.remove(0);
                }
            }
            dataSource.add(value);
            ca.set(storeKey, dataSource.toJSON());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object first() {
        updateStore();
        var l = dataSource.size();
        return l > 0 ? dataSource.get(l - 1) : null;
    }

    public List<Object> news(int index) {
        updateStore();
        List<Object> r = new ArrayList<>();
        for (int i = index, l = dataSource.size(); i < l; i++) {
            if (i < l) {
                r.add(dataSource.get(i));
            }
        }
        return r;
    }

    public void clear() {
        dataSource.clear();
        getCacheHelper().set(storeKey, DataSourceTemplate.build().toJSON());
    }

    public int size() {
        return updateStore().size();
    }

    // 获得全部数据
    public List<Object> all() {
        return updateStore().toArrayList();
    }

    public DataSourceDistribution newInstance() {
        DataSourceDistribution r = new DataSourceDistribution();
        r.clear();
        return r;
    }

    public void close() {
        // 直接删除 key
        getCacheHelper().delete(storeKey);
    }

    public boolean isInvalid() {
        return getCacheHelper().get(storeKey) == null;
    }

    public void onOverflow(Consumer<DataSourceTemplate> fn) {
        this.overflow_fn = fn;
    }

    public void setGetOverflow(IGetOverflowResult fn) {
        this.getAll_fn = fn;
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public List<Object> all(int start, int end) {
        // 获得全部数据定义了，从原始数据源拿
        if (getAll_fn != null) {
            return getAll_fn.call(start, end);
        }
        // 没有定义，从本地数据源拿
        if ((start + end) <= dataSource.size()) {
            return dataSource.toArrayList().subList(start, end);
        }
        // 本地数据不够，尽可能给符合要求的
        var l = dataSource.toArrayList();
        return l.subList(start, l.size() - 1);
    }
}
