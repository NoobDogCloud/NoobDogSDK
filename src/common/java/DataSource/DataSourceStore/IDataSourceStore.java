package common.java.DataSource.DataSourceStore;

import java.util.List;
import java.util.function.Consumer;

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

    // 获得有效缓存数据长度
    int size();

    // 创建本身类新实例
    IDataSourceStore newInstance();

    // 设置数据源
    void close();

    // 数据源是否无效
    boolean isInvalid();

    // 数据源存储数量溢出
    void onOverflow(Consumer<DataSourceTemplate> fn);

    // 获得数据源存储数量
    int getMaxSize();

    // 设置对多储存多少数据
    void setMaxSize(int maxSize);

    // 获得数据源存储数量
    List<Object> all(int start, int end);

    // 获得已溢出的数据
    void setGetOverflow(IGetOverflowResult fn);
}
