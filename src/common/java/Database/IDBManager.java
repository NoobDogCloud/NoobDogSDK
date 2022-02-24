package common.java.Database;

public interface IDBManager<T> extends IDBLayer<T> {
    String[] getAllTables();

    String tableBuildMeta(String tableName);

    boolean buildTableFromMeta(String tableName, String buildMeta);

    boolean removeTable(String tableName);
}
