package common.java.Database;

public interface IDBManager<T> extends IDBLayer<T> {

    String tableBuildMeta(String tableName);

    boolean buildTableFromMeta(String tableName, String buildMeta);
}
