package common.java.DataSource.DataSourceStore;

import java.util.ArrayList;
import java.util.List;

public class DataSourceLocal<T> implements IDataSourceStore<T> {
    private final List<T> dataSource;

    private DataSourceLocal() {
        dataSource = new ArrayList<>();
    }

    public static <T> DataSourceLocal<T> build() {
        return new DataSourceLocal<>();
    }

    public boolean add(T value) {
        return dataSource.add(value);
    }

    public T first() {
        var l = dataSource.size();
        return l > 0 ? dataSource.get(l - 1) : null;
    }

    public List<T> news(int index) {
        List<T> r = new ArrayList<>();
        for (int i = index, l = dataSource.size(); i < l; i++) {
            if (i < l) {
                r.add(dataSource.get(i));
            }
        }
        return r;
    }

    public void clear() {
        dataSource.clear();
    }

    public int size() {
        return dataSource.size();
    }

    // 获得全部数据
    public List<T> all() {
        return dataSource;
    }
}
