package common.java.DataSource.DataSourceStore;

import common.java.Cache.CacheHelper;

import java.util.ArrayList;
import java.util.List;

public class DataSourceDistribution implements IDataSourceStore {
    private CacheHelper ca;
    private String storeKey;
    private DataSourceTemplate dataSource;

    private DataSourceDistribution() {
        this.dataSource = DataSourceTemplate.build();
    }

    public static DataSourceDistribution build() {
        return new DataSourceDistribution();
    }

    private CacheHelper updateStore() {
        if (ca == null) {
            ca = CacheHelper.buildForApp();
        }
        if (storeKey == null) {
            storeKey = ca.generateUniqueKey("custom_store_subscribe");
        }
        dataSource = DataSourceTemplate.build(ca.getJson(storeKey));
        return ca;
    }

    public boolean add(Object value) {
        try {
            dataSource.add(value);
            ca.set(storeKey, dataSource);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object first() {
        // updateStore();
        var l = dataSource.size();
        return l > 0 ? dataSource.get(l - 1) : null;
    }

    public List<Object> news(int index) {
        // updateStore();
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
        updateStore().set(storeKey, "[]");
    }

    public int size() {
        updateStore();
        return dataSource.size();
    }

    // 获得全部数据
    public List<Object> all() {
        // updateStore();
        return dataSource.toArrayList();
    }

    public DataSourceDistribution newInstance() {
        DataSourceDistribution r = new DataSourceDistribution();
        r.clear();
        return r;
    }

    public void close() {
        // 直接删除 key
        updateStore().delete(storeKey);
    }

    public boolean isInvalid() {
        return updateStore().get(storeKey) == null;
    }
}
