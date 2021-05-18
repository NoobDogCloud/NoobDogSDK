package common.java.Database;

public class SqlFieldProp {
    private final String preType = "";
    private final String colName;
    private boolean isNull = false;
    private boolean isAutoIncrement = false;
    private boolean primaryKey = false;
    private String typeName = "";
    private Object defaultValue = null;

    public SqlFieldProp(String colName) {
        this.colName = colName;
    }

    public String build() {
        String tempSql = "";
        if (colName != null && typeName != null) {
            tempSql = "DEFAULT '" + defaultValue.toString() + "' ";
        }
        return tempSql;
    }

    //------------------------------
    public SqlFieldProp INT() {
        return initType("int", 10, -1, true, false);
    }

    public SqlFieldProp TINYINT() {
        return initType("tinyint", 3, -1, true, false);
    }

    public SqlFieldProp SMALLINT() {
        return initType("smallint", 6, -1, true, false);
    }

    public SqlFieldProp MEDIUMINT() {
        return initType("mediumint", 8, -1, true, false);
    }

    public SqlFieldProp BIGINT() {
        return initType("bigint", 20, -1, true, false);
    }

    public SqlFieldProp FLOAT() {
        return initType("float", 0, -1, false, false);
    }

    public SqlFieldProp DOUBLE() {
        return initType("double", 0, -1, false, false);
    }

    public SqlFieldProp DECIMAL() {
        return initType("decimal", 10, 0, false, false);
    }

    public SqlFieldProp initType(String typeName, int len, int init, boolean unsgined, boolean zerofill) {
        return this;
    }

    public SqlFieldProp VARCHAR() {
        return VARCHAR(10, false);
    }

    public SqlFieldProp TINYBLOB() {
        return initType("tinyblob", 0, -1, false, false);
    }

    public SqlFieldProp TINYTEXT() {
        return initType("tinytext", 0, -1, false, false);
    }

    public SqlFieldProp BLOB() {
        return initType("blob", 0, -1, false, false);
    }

    public SqlFieldProp TEXT() {
        return initType("text", 0, -1, false, false);
    }

    public SqlFieldProp MEDIUMBLOB() {
        return initType("mediumblob", 0, -1, false, false);
    }

    public SqlFieldProp MEDIUMTEXT() {
        return initType("meduimtext", 0, -1, false, false);
    }

    public SqlFieldProp LONGBLOB() {
        return initType("longblob", 0, -1, false, false);
    }

    public SqlFieldProp LONGTEXT() {
        return initType("longtext", 0, -1, false, false);
    }

    public SqlFieldProp DATE() {
        return initType("date", 0, -1, false, false);
    }

    public SqlFieldProp TIME() {
        return initType("time", 0, -1, false, false);
    }

    public SqlFieldProp YEAR() {
        return initType("year", 0, -1, false, false);
    }

    public SqlFieldProp DATETIME() {
        return initType("datetime", 0, -1, false, false);
    }

    public SqlFieldProp TIMESTAMP() {
        return initType("timestamp", 0, -1, false, false);
    }

    //------------------------------

    public SqlFieldProp VARCHAR(int len, boolean binary) {
        typeName = "varchar(" + len + ")" + " " + (binary ? "CHARACTER SET utf8 COLLATE utf8_bin" : "");
        return this;
    }

    public SqlFieldProp setDefaultValue(Object obj) {
        defaultValue = obj;
        return this;
    }

    public boolean isNull() {
        return isNull;
    }

    public SqlFieldProp isNull(boolean flag) {
        isNull = flag;
        return this;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public SqlFieldProp isAutoIncrement(boolean flag) {
        isAutoIncrement = flag;
        return this;
    }

    public boolean primaryKey() {
        return primaryKey;
    }

    public SqlFieldProp primaryKey(boolean flag) {
        primaryKey = flag;
        return this;
    }

}
