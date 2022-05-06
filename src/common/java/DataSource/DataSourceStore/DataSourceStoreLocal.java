package common.java.DataSource.DataSourceStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DataSourceStoreLocal implements IDataSourceStore {
    private final DataSourceTemplate dataSource;
    private boolean running = true;
    private Consumer<DataSourceTemplate> overflow_fn;
    private IGetOverflowResult getAll_fn;
    private int maxSize = 500;

    private DataSourceStoreLocal() {
        dataSource = DataSourceTemplate.build();
    }

    public static DataSourceStoreLocal build() {
        return new DataSourceStoreLocal();
    }

    // 如果溢出未设置这里采用滚动数据模式
    public boolean add(Object value) {
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
        return true;
    }

    public Object first() {
        var l = dataSource.size();
        return l > 0 ? dataSource.get(l - 1) : null;
    }

    public List<Object> news(int index) {
        List<Object> r = new ArrayList<>();
        for (int i = index, l = dataSource.size(); i < l; i++) {
            r.add(dataSource.get(i));
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
