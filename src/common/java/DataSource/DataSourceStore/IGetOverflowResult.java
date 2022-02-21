package common.java.DataSource.DataSourceStore;

import java.util.List;

public interface IGetOverflowResult {
    List<Object> call(int start, int length);
}
