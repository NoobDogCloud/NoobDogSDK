package common.java.DataSource.DataSourceStore;

import java.util.List;

public interface IDataSourceStore {
    // 添加数据
    boolean add(Object value);

    // 获得最新数据
    Object first();

    // 获得最新未读数据
    List<Object> news(int start);

    List<Object> all();

    // 清空数据
    void clear();

    // 获得数据长度
    int size();

    // 创建本身类新实例
    IDataSourceStore newInstance();

    // 设置数据源
    void close();

    // 数据源是否无效
    boolean isInvalid();
}
