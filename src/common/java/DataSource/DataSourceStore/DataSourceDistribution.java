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
}
