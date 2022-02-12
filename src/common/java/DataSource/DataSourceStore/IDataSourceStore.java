package common.java.DataSource.DataSourceStore;

import java.util.List;

public interface IDataSourceStore<T> {
    // 添加数据
    boolean add(T value);

    // 获得最新数据
    T first();

    // 获得最新未读数据
    List<T> news(int start);

    List<T> all();

    // 清空数据
    void clear();

    // 获得数据长度
    int size();
}
