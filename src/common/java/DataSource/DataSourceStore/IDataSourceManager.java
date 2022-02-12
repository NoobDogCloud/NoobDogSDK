package common.java.DataSource.DataSourceStore;

public interface IDataSourceManager<T> {
    IDataSourceStore<T> get(String topic);

    void add(String topic, IDataSourceStore<T> dataSource);

    void remove(String topic);

    boolean contains(String topic);
}
