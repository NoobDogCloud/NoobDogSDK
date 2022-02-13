package common.java.DataSource.DataSourceStore;

import java.util.ArrayList;
import java.util.List;

public class DataSourceStoreLocal implements IDataSourceStore {
    private final DataSourceTemplate dataSource;
    private boolean running = true;

    private DataSourceStoreLocal() {
        dataSource = DataSourceTemplate.build();
    }

    public static DataSourceStoreLocal build() {
        return new DataSourceStoreLocal();
    }

    public boolean add(Object value) {
        dataSource.add(value);
        return true;
    }

    public Object first() {
        var l = dataSource.size();
        return l > 0 ? dataSource.get(l - 1) : null;
    }

    public List<Object> news(int index) {
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
    }

    public int size() {
        return dataSource.size();
    }

    // 获得全部数据
    public List<Object> all() {
        return dataSource.toArrayList();
    }

    public DataSourceStoreLocal newInstance() {
        return new DataSourceStoreLocal();
    }

    public void close() {
        running = false;
    }

    // 数据源是否失效
    public boolean isInvalid() {
        return !running;
    }
}
