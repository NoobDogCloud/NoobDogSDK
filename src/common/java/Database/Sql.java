package common.java.Database;

import com.zaxxer.hikari.HikariDataSource;
import common.java.EventWorker.Worker;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/***
 * mysql配置解析
 {
 "dbName": "mysql",
 "user": "",
 "password": "",
 "database": "grape_cloud_dev",
 "initsize": 2,
 "maxactive": 200,
 "minidle": 0,
 "maxwait": 60000,
 "validationquery": "select 1",
 "testinborrow": true,
 "testwhileidle": true,
 "poolpreparedstatements": false,
 "useUnicode": true,
 "characterEncoding": "UTF-8",
 "nodeAddresses": ["mysql://putao282.mysql.rds.aliyuncs.com:3306"]
 // com.mysql.jdbc.Driver
 // com.mysql.cj.jdbc.Driver
 }
 *
 */

public class Sql implements IDBManager<Sql> {
    /**
     *
     */
    protected static final HashMap<String, HikariDataSource> DataSource;


    static {
        DataSource = new HashMap<>();
    }

    protected final String _configString;
    protected final HashMap<String, Object> constantCondition;
    protected boolean effectDirty = false;
    protected HikariDataSource dataSource;//  = new DruidDataSource();
    //声明线程共享变量
    protected boolean conditionLogicAnd;
    protected int skipNo;
    protected int limitNo;
    protected JSONObject sortBSON;
    protected boolean _count;
    protected boolean _max;
    protected boolean _min;
    protected boolean _sum;
    protected boolean _avg;
    protected boolean _distinct;
    protected List<List<Object>> conditionJSON;    //条件
    protected List<JSONObject> dataJSON;
    protected Set<String> fieldVisible;
    protected Set<String> fieldDisable;

    protected String groupByField = "";
    protected String formName;
    protected String ownId;
    protected HashMap<String, String> baseTable;        //基本表信息,准备用
    protected HashMap<String, Boolean> tableState;      //派生表状态，加速用
    protected List<String> tableFields;                 //表字段结构
    protected String pkName;                            //表主键
    protected boolean isDirty;

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    public Sql(String configString) {
        isDirty = false;
        formName = "";
        _configString = configString;
        constantCondition = new HashMap<>();
        initsql();
        reinit();
    }

    public static String TransactSQLInjection(String str) {
        //System.out.println(str);
        return str;//.replaceAll(".*(';|).*", " ");}
    }

    /***获取当前线程上的连接开启事务*/
    public void startTransaction() {
        Connection conn = getNewConnection();
        if (conn == null) {//如果连接为空
            //container.set(conn);//将此连接放在当前线程上
            nLogger.logInfo(Thread.currentThread().getName() + "空连接从dataSource获取连接");
        } else {
            nLogger.logInfo(Thread.currentThread().getName() + "从缓存中获取连接");
        }
        try {
            Objects.requireNonNull(conn).setAutoCommit(false);//开启事务
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            _Close(conn);
        }
    }

    protected void initsql() {
        JSONObject obj;
        String user;
        String password;
        String databaseName;
        boolean useUnicode;
        boolean useSSL;
        String charName;
        int minIdle;
        int maxWait;
        int maxActive;
        String className;
        String splitChar;
        String startChar;
        try {
            obj = JSONObject.toJSON(_configString);
            // 查看连接池驱动名
            // https://github.com/brettwooldridge/HikariCP
            className = obj.getString("class");
            user = obj.getString("user");
            password = obj.getString("password");
            databaseName = obj.getString("database");
            charName = obj.containsKey("characterEncoding") ? obj.getString("characterEncoding") : null;
            useUnicode = obj.containsKey("useUnicode") && obj.getBoolean("useUnicode");
            useSSL = obj.containsKey("useSSL") && obj.getBoolean("useSSL");
            splitChar = obj.containsKey("spilt") ? obj.getString("spilt") : "&";
            startChar = obj.containsKey("start") ? obj.getString("spilt") : "?";
            // 注意url串中间不要有空格，因为mysql源码对多个地址split时没有trim.(replication mode)
            // " jdbc:mysql:replication://127.0.0.1:3309,127.0.0.1:3306/core " ,

            String url = obj.getString("host") + "/" + databaseName;
            String extern = "";
            if (useSSL) {
                extern += (splitChar + "useSSL=true");
            }
            if (useUnicode) {
                extern += (splitChar + "useUnicode=true");
            }
            if (charName != null) {
                extern += (splitChar + "characterEncoding=" + charName);
            }
            // mysql 必须有
            if (obj.containsKey("timezone")) {
                extern += (splitChar + "serverTimezone=" + obj.getString("timezone"));
            }
            if (obj.containsKey("database_to_upper")) {
                extern += (splitChar + "DATABASE_TO_UPPER=" + obj.getBoolean("database_to_upper"));
            }
            if (obj.containsKey("database_to_lower")) {
                extern += (splitChar + "DATABASE_TO_LOWER=" + obj.getBoolean("database_to_lower"));
            }
            if (obj.containsKey("case_insensitive_identifiers")) {
                extern += (splitChar + "CASE_INSENSITIVE_IDENTIFIERS=" + obj.getBoolean("case_insensitive_identifiers"));
            }
            if (obj.containsKey("ignore_case")) {
                extern += (splitChar + "IGNORECASE=" + obj.getBoolean("ignore_case"));
            }
            if (obj.containsKey("mode")) {
                extern += (splitChar + "MODE=" + obj.getString("mode"));
            }
            if (obj.containsKey("auto_reconnect")) {
                extern += (splitChar + "AUTO_RECONNECT=" + obj.getBoolean("auto_reconnect"));
            }

            if (extern.length() > 0) {
                extern = (startChar + extern.substring(1));
                url += extern;
            }

            minIdle = obj.getInt("minidle");
            maxWait = obj.getInt("maxwait");
            maxActive = obj.getInt("maxactive");

            HikariDataSource ds = DataSource.getOrDefault(_configString, null);
            if (ds == null || ds.isClosed() || !ds.isRunning()) {
                ds = new HikariDataSource();
                if (!user.equals("")) {
                    ds.setUsername(user);
                    ds.setPassword(password);
                    ds.setConnectionTestQuery("select 1");
                    ds.setMaximumPoolSize(maxActive);
                    ds.setKeepaliveTime(7200);
                    ds.setMaxLifetime(1800000);
                    ds.setMinimumIdle(minIdle);
                    ds.setMaxLifetime(maxWait);
                    ds.setJdbcUrl("jdbc:" + url);
                    ds.setLoginTimeout(5);
                    if (!StringHelper.isInvalided(className)) {
                        ds.setDriverClassName(className);
                    }
                }
                DataSource.put(_configString, ds);
            }
            //nLogger.logInfo("[SQL]nowCnt:" + ds.getActiveCount() + " closedCnt:" + ds.getCloseCount() + " connectCnt:" + ds.getConnectCount() + " connectErrorCnt:" + ds.getConnectErrorCount());
            dataSource = ds;
        } catch (Exception e) {
            nLogger.logInfo(e, "Sql server Config node error!");
            nLogger.logInfo("Config:" + _configString);
        }
        tableFields = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max) {
        if (func == null) {
            nLogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nLogger.logInfo("scan 每页最大值不能小于等于0");
        }

        int maxCount = (int) dirty().count();
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        JSONArray jsonArray, tempResult;
        tempResult = new JSONArray();
        for (int index = 1; index <= pageNO; index++) {
            jsonArray = dirty().page(index, max);
            assert func != null;
            jsonArray = func.apply(jsonArray);
            if (jsonArray != null) {
                tempResult.addAll(jsonArray);
            }
        }
        return tempResult;
    }

    /**
     * 多线程同步扫描
     *
     * @param func
     * @param max
     * @param synNo
     * @return
     */
    @SuppressWarnings("unchecked")
    public JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max, int synNo) {
        if (func == null) {
            nLogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nLogger.logInfo("scan 每页最大值不能小于等于0");
            max = 1;
        }
        if (synNo <= 0) {
            nLogger.logInfo("scan 同步执行不能小于等于0");
        }

        int index;
        long rl = dirty().count();
        int maxCount = (int) rl;
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        ConcurrentHashMap<Integer, JSONArray> tempResult;
        tempResult = new ConcurrentHashMap<>();

        List<List<Object>> condJSON = getCond();
        String _formName = getForm();
        for (index = 1; index <= pageNO; index++) {
            final int _index = index;
            final int _max = max;
            threadPool.execute(() -> {
                try {
                    Sql db = new Sql(_configString);
                    db.form(_formName);
                    db.setCond(condJSON);
                    JSONArray<JSONObject> jsonArray = db.page(_index, _max);
                    var result = func.apply(jsonArray);
                    if (result != null) {
                        tempResult.put(_index, result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }


        JSONArray rArray = new JSONArray();
        for (int key : tempResult.keySet()) {
            rArray.addAll(tempResult.get(key));
        }
        return rArray;
    }


    //public Connection getNewConnection(){
    public Connection getNewConnection() {
        //return pool;
        Connection con = null;
        try {
            con = dataSource.getConnection();
        } catch (SQLException e) {
            nLogger.logInfo(e);
        }
        return con;
    }

    protected void _Close(Connection conn) {
        try {
            if (conn != null) {//如果连接不为空
                //conn.close();
                conn.close();
            }
        } catch (SQLException e) {
            nLogger.logInfo(e);
        }
    }

    /**
     * 获取数据连接
     *
     */
    //提交事务
    public void commit() {
        Connection conn = getNewConnection();
        try {
            if (null != conn) {
                conn.commit();//提交事务
                // nLogger.logInfo(Thread.currentThread().getName()+"事务已经提交......");
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            _Close(conn);
        }
    }

    /***回滚事务*/
    public void rollback() {
        Connection conn = getNewConnection();
        try {
            if (conn != null) {
                conn.rollback();//回滚事务
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            _Close(conn);
        }
    }

    /***关闭连接*/
    protected void reinit() {
        if (isDirty) {//脏执行下不重置
            isDirty = false;
            effectDirty = true;
            return;
        }
        if (fieldVisible == null) {
            conditionJSON = new ArrayList<>();
        } else {
            conditionJSON.clear();
        }

        effectDirty = false;
        conditionLogicAnd = true;
        if (fieldVisible == null) {
            fieldVisible = new HashSet<>();
        } else {
            fieldVisible.clear();
        }
        if (fieldDisable == null) {
            fieldDisable = new HashSet<>();
        } else {
            fieldDisable.clear();
        }

        if (sortBSON == null) {
            sortBSON = new JSONObject();//默认_id排序
        } else {
            sortBSON.clear();
        }

        if (dataJSON == null) {
            dataJSON = new ArrayList<>();
        } else {
            dataJSON.clear();
        }

        limitNo = 0;
        skipNo = 0;

        _count = false;
        _max = false;
        _min = false;
        _sum = false;
        _avg = false;

        _distinct = false;

        ownId = null;

        and();

        if (tableState == null) {
            tableState = new HashMap<>();
        } else {
            tableState.clear();
        }
        if (baseTable == null) {
            baseTable = new HashMap<>();
        } else {
            baseTable.clear();
        }
    }

    /**
     * 获得主键，并且构造表结构
     *
     * @return
     */
    public String getGeneratedKeys() {
        if (tableFields.size() < 1) {
            Connection conn = getNewConnection();
            try {
                DatabaseMetaData DBM = conn.getMetaData();
                //ResultSet tableRet = DBM.getTables(null, "%", getform(),new String[]{"TABLE"});
                String tableName = _getFullForm();
                // ResultSet colRet = DBM.getColumns(null, "%", tableName, "%");
                ResultSet colRet = DBM.getPrimaryKeys(null, "%", tableName);
                if (colRet.next()) {
                    pkName = colRet.getString("COLUMN_NAME");
                } else {
                    pkName = "id";
                }

                ResultSet colResult = DBM.getColumns(null, "%", tableName, "%");
                while (colResult.next()) {
                    tableFields.add(colResult.getString("COLUMN_NAME"));
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                nLogger.logInfo(e);
            } finally {
                _Close(conn);
            }
        }
        return pkName;
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        and();
        constantCondition.put(fieldName, CondValue);
    }

    public void delConstantCond(String fieldName) {
        constantCondition.remove(fieldName);
    }

    public Sql and() {
        conditionLogicAnd = true;
        return this;
    }

    public Sql or() {
        conditionLogicAnd = false;
        return this;
    }

    public Sql eq(String field, Object value) {//One Condition
        addCondition(field, value, "=");
        return this;
    }

    public Sql ne(String field, Object value) {//One Condition
        addCondition(field, value, "!=");
        return this;
    }

    public Sql gt(String field, Object value) {//One Condition
        addCondition(field, value, ">");
        return this;
    }

    public Sql lt(String field, Object value) {//One Condition
        addCondition(field, value, "<");
        return this;
    }

    public Sql gte(String field, Object value) {//One Condition
        addCondition(field, value, ">=");
        return this;
    }

    public Sql lte(String field, Object value) {//One Condition
        addCondition(field, value, "<=");
        return this;
    }

    public Sql like(String field, Object value) {//One Condition
        addCondition(field, "%" + value.toString() + "%", "like");
        return this;
    }

    /**
     * 判断条件是否为空
     *
     * @return
     */
    public boolean nullCondition() {
        return conditionJSON.size() == 0;
    }

    public Sql where(List<List<Object>> condArray) {
        conditionJSON.addAll(condArray);
        return this;
    }

    public Sql where(JSONArray condArray) {
        JSONObject tmpJSON;
        String field, logic, link_login;
        Object value;
        if (condArray == null) {
            return null;
        }

        if (condArray.size() > 0) {
            for (Object jObject : condArray) {
                field = null;
                logic = null;
                value = null;
                link_login = null;
                tmpJSON = (JSONObject) jObject;
                if (tmpJSON.containsKey("logic")) {
                    logic = (String) tmpJSON.get("logic");
                }
                if (tmpJSON.containsKey("value")) {
                    value = tmpJSON.get("value");
                }
                if (tmpJSON.containsKey("field")) {
                    field = (String) tmpJSON.get("field");
                }
                if (tmpJSON.containsKey("link_logic")) {
                    link_login = (String) tmpJSON.get("link_logic");
                }
                if (logic != null && field != null) {
                    if (link_login == null) {
                        addCondition(field, value, logic);
                    } else {
                        addCondition(field, value, logic, link_login.equalsIgnoreCase("and"));
                    }
                } else {
                    nLogger.errorInfo(condArray + " ->输入的 条件对象无效");
                }
            }
            return this;
        }
        return null;
    }

    protected <T> void addCondition(String field, T value, String logic) {
        addCondition(field, value, logic, conditionLogicAnd);
    }

    protected <T> void addCondition(String field, T value, String logic, boolean link_logic) {
        List<Object> nJSONArray;
        if (value != null && !value.toString().equals("")) {
            nJSONArray = new ArrayList<>();
            nJSONArray.add(link_logic ? "and" : "or");
            nJSONArray.add(field);
            if (logic.equals("==")) {
                logic = "=";
            }
            nJSONArray.add(logic);
            nJSONArray.add(value);
            conditionJSON.add(nJSONArray);
        }
    }

    public Sql data(String jsonString) {
        data(JSONObject.toJSON(jsonString));

        return this;
    }

    public Sql data(JSONObject doc) {
        dataJSON.add(doc);
        return this;
    }

    public List<JSONObject> clearData() {
        List<JSONObject> v = new ArrayList<>(data());
        dataJSON.clear();
        return v;
    }

    public List<JSONObject> data() {
        return dataJSON;
    }

    public Sql field(String fieldString) {
        field(fieldString.split(","));
        return this;
    }

    public Sql field() {
        fieldVisible.clear();
        fieldDisable.clear();
        return this;
    }

    protected String fieldSQL() {
        if (fieldVisible.size() == 0) {
            return "*";
        } else {
            StringBuilder rs = new StringBuilder();
            for (String val : fieldVisible) {
                rs.append("`").append(val).append("`,");
            }
            rs = new StringBuilder(StringHelper.build(rs.toString()).removeTrailingFrom().toString());
            return rs.toString();
        }
    }

    public Sql field(String[] _fieldList) {
        fieldVisible.addAll(Arrays.asList(_fieldList));
        return this;
    }

    public Sql mask(String fieldString) {
        String[] maskField = fieldString.split(",");
        return mask(maskField);
    }

    public Sql mask(String[] _fieldList) {
        fieldDisable.addAll(Arrays.asList(_fieldList));
        return this;
    }

    protected String result2create(ResultSet rst) {
        ResultSetMetaData m;
        int columns, n;
        String fieldName, fieldType;
        StringBuilder tmpStr = null;
        try {
            m = rst.getMetaData();//获取 列信息
            columns = m.getColumnCount();
            if (columns > 0) {
                tmpStr = new StringBuilder("(");
                for (int i = 1; i <= columns; i++) {
                    fieldName = m.getColumnName(i);
                    fieldType = m.getColumnTypeName(i);
                    n = m.getColumnDisplaySize(i);
                    //fieldName += " NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
                    switch (fieldType) {
                        case "INT UNSIGNED" -> fieldType = "INT(" + n + ") UNSIGNED";
                        case "TIMESTAMP" -> fieldType += " NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
                        default -> fieldType = fieldType + "(" + n + ")";
                    }
                    if (m.isAutoIncrement(i)) {
                        fieldType += " auto_increment primary key";
                    }
                    //create table if not exists people(name text,age int(2),gender char(1));
                    tmpStr.append(fieldName).append(" ").append(fieldType).append(",");
                }
                tmpStr = new StringBuilder(StringHelper.build(tmpStr.toString()).removeTrailingFrom().toString() + ")");
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            nLogger.logInfo(e);
        }
        assert tmpStr != null;
        return tmpStr.toString();
    }

    protected String getCreateSQL(String tableName) {
        ResultSet rs;
        Statement smt;
        String sqlString = null;
        Connection conn = getNewConnection();
        try {
            smt = conn.createStatement();
            String sql = "select * from " + tableName + " limit 1";
            rs = smt.executeQuery(sql);
            if (rs != null) {
                sqlString = result2create(rs);
                baseTable.put(tableName, sqlString);
            }
        } catch (SQLException e) {
            nLogger.logInfo(e);
        } finally {
            _Close(conn);
        }
        return sqlString;
    }

    protected boolean createTable(String tableName, String colString) {
        boolean rs = false;
        String sql;
        if (colString != null) {
            Connection conn = getNewConnection();
            try {
                sql = "create table if not exists " + tableName + colString + ("");
                Statement smt = conn.createStatement();
                //nLogger.logInfo(Sql);
                rs = smt.execute(sql);
                tableState.put(tableName, true);
            } catch (SQLException e) {
                nLogger.logInfo(e);
                //创建表单异常
                rs = false;
            } finally {
                _Close(conn);
            }
        }
        return rs;
    }

    protected String getCreateTableColSQL() {
        JSONObject json;
        String rString = "";
        if (dataJSON.size() > 0) {
            json = dataJSON.get(0);
            SqlFieldProp fp;
            Object temp;
            StringBuilder newsql = new StringBuilder();
            for (Object key : json.keySet()) {
                temp = json.get(key);
                if (temp instanceof SqlFieldProp) {
                    fp = (SqlFieldProp) temp;
                    newsql.append(fp.build()).append(",");
                } else {
                    nLogger.logInfo("错误表字段描述");
                }
            }
            if (!newsql.toString().equals("")) {
                rString = "(" + StringHelper.build(newsql.toString()).trimFrom(',').toString() + ")";
            }
        }
        return rString;
    }

    public boolean newTable() {
        boolean rb = false;
        String createSQL = getCreateTableColSQL();
        if (!createSQL.equals("")) {
            rb = createTable(getFullForm(), createSQL);
        }
        reinit();
        return rb;
    }

    public boolean newTempTable() {
        boolean rb = false;
        String createSQL = getCreateTableColSQL();
        if (!createSQL.equals("")) {
            rb = createTable(getFullForm(), createSQL);
        }
        reinit();
        return rb;
    }

    public String tableBuildMeta(String tableName) {
        return getCreateSQL(tableName);
    }

    public boolean buildTableFromMeta(String tableName, String buildMeta) {
        return createTable(tableName, buildMeta);
    }

    public boolean removeTable(String tableName) {
        boolean rb = false;
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            String sql = "drop table if exists " + tableName;
            rb = smt.execute(sql);
        } catch (SQLException e) {
            nLogger.logInfo(e);
        } finally {
            _Close(conn);
        }
        return rb;
    }

    /**
     * 判断表是否存在，不存在就创建
     */
    protected boolean safeTable() {
        /*
         * 1:获得基础表的字段信息
         * 2:生成创建SQL
         * 3:填入缓存
         * */
        if (ownId == null || ownId.equals("")) {
            return true;
        }
        boolean rs = true;
        String baseName = formName;
        String ownId = this.ownId;
        String createTableSql;
        String newTable = baseName + "_" + ownId;
        if (!tableState.containsKey(newTable)) {
            if (baseTable.containsKey(baseName)) {
                createTableSql = baseTable.get(baseName);
            } else {
                createTableSql = getCreateSQL(baseName);
            }
            rs = createTable(newTable, createTableSql);
        }
        return rs;
    }

    public Sql form(String _formName) {
        formName = _formName;
        safeTable();
        return this;
    }

    public String getFullForm() {
        return ownId == null || ownId.equals("") ? formName : formName + "_" + ownId;
    }

    private String _getFullForm() {
        return ownId == null || ownId.equals("") ? formName : formName + "_" + ownId;
    }

    public String getForm() {
        return formName;
    }

    public Sql skip(int no) {
        skipNo = no;
        return this;
    }

    public Sql limit(int no) {
        limitNo = no;
        return this;
    }

    public Sql asc(String field) {
        sortBSON.put(field, "asc");
        return this;
    }

    public Sql desc(String field) {
        sortBSON.put(field, "desc");
        return this;
    }

    public List<Object> insert() {
        ResultSet rs;
        List<Object> rList = new ArrayList<>();
        Connection conn = getNewConnection();
        try {
            List<String> lStrings = insertSQL();
            Statement smt = conn.createStatement();
            for (String _sql : lStrings) {
                String nsql = TransactSQLInjection(_sql);
                // nLogger.logInfo(nsql);
                smt.execute(nsql, Statement.RETURN_GENERATED_KEYS);
                rs = smt.getGeneratedKeys();
                if (rs != null && rs.next()) {
                    rList.add(rs.getObject(1));
                }
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return rList;
    }

    public void asyncInsert() {
        List<String> lStrings = insertSQL();
        try {
            Worker.submit(() -> {
                Statement smt;
                Connection conn = getNewConnection();
                try {
                    smt = conn.createStatement();
                    int i, l = lStrings.size();
                    for (i = 0; i < l; i++) {
                        smt.executeUpdate(TransactSQLInjection(lStrings.get(i)));
                    }
                    smt.close();
                } catch (Exception e1) {
                    nLogger.logInfo(e1);
                } finally {
                    _Close(conn);
                }
            });
        } finally {
            reinit();
        }
    }

    public Sql clearResult() {

        return this;
    }

    public Object insertOnce() {
        String sqlString = "";
        ResultSet rs;
        Object rObject = null;
        Connection conn = getNewConnection();
        try {
            List<String> lStrings = insertSQL();
            //System.out.println(lStrings.toString());
            Statement smt = conn.createStatement();
            sqlString = lStrings.get(0);
            if (sqlString != null && !sqlString.equals("")) {
                String nsql = TransactSQLInjection(sqlString);
                // nLogger.logInfo(nsql);
                smt.execute(nsql, Statement.RETURN_GENERATED_KEYS);
                rs = smt.getGeneratedKeys();
                if (rs != null) {
                    if (rs.next()) {
                        rObject = rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            nLogger.logInfo(e, sqlString);
        } finally {
            reinit();
            _Close(conn);
        }
        return rObject;
    }

    public JSONObject getAndUpdate() {
        try {
            var rs = (JSONObject) (((JSONArray<?>) _findex(false)).get(0));
            _update(false);
            return rs;
        } catch (Exception e) {
            return null;
        } finally {
            reinit();
        }
    }

    public boolean update() {//atom后返回当前值再修改
        return _update(false) > 0;
    }

    public long updateAll() {
        return _update(true);
    }

    protected long _update(boolean isall) {//缺少特殊update政策支持,原子模式下模拟未实现
        long rs = 0;
        List<String> lStrings = updateSQL();
        Statement smt;
        Connection conn = getNewConnection();
        try {
            smt = conn.createStatement();
            for (String _sql : lStrings) {
                String nsql = TransactSQLInjection(_sql + (isall ? "" : " limit 1"));
                // nLogger.logInfo(nsql);
                rs += smt.executeUpdate(nsql);
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return rs;
    }

    protected List<String> insertSQL() {
        List<String> sqlList = new ArrayList<>();
        for (JSONObject _t : dataJSON) {//dataJSON可以包含多个jsonString,每一个jsonString代表一个操作
            StringBuilder fieldString = new StringBuilder();
            StringBuilder valueString = new StringBuilder();
            for (Object _j : _t.keySet()) {//为每一个jsonString构造k-v insertsql
                fieldString.append("`").append(_j.toString()).append("`,");
                valueString.append(sqlvalue(_t.get(_j))).append(",");
            }
            sqlList.add("insert into " + getFullForm() + "(" + StringHelper.build(fieldString.toString()).removeTrailingFrom().toString() + ")" + " values(" + StringHelper.build(valueString.toString()).removeTrailingFrom().toString() + ")");
            //sqlList.add("insert into " + formName + " values(" + StringHelper.killlast(valueString) + ")");
        }
        return sqlList;
    }

    public List<List<Object>> getCond() {
        return conditionJSON;
    }

    public Sql setCond(List<List<Object>> conJSON) {
        conditionJSON = conJSON;
        return this;
    }

    public Sql groupWhere(JSONArray condArray) {
        return groupCondition(DBFilter.buildDbFilter(condArray).buildEx());
    }

    public Sql groupCondition(List<List<Object>> conds) {
        if (conds != null && conds.size() > 0) {
            List<Object> block = new ArrayList<>();
            block.add(conditionLogicAnd ? "and" : "or");
            block.add(conds);
            conditionJSON.add(block);
        }
        return this;
    }

    protected String whereSQL() {//不支持自由条件，必须严格区分and和or2个组
        StringBuilder rString = new StringBuilder();
        // 补充恒定条件
        if (constantCondition.size() > 0 && !effectDirty) {
            and();
            for (String field : constantCondition.keySet()) {
                eq(field, constantCondition.get(field));
            }
        }
        if (conditionJSON.size() > 0) {
            int cnt = 0;
            for (List<Object> item : conditionJSON) {
                rString.append(whereSQL(item, cnt == 0));
                cnt++;
            }
            rString.insert(0, " where ");
        }
        return rString.toString();
    }

    protected String whereSQL(List<Object> conds, boolean isfirst) {
        StringBuilder r = new StringBuilder();
        int cnt = 0;
        for (Object item : conds) {
            Object idx0 = conds.get(0);
            if (item instanceof ArrayList) {//列表对象是list
                List<Object> info = (List<Object>) item;
                r.append(whereSQL(info, cnt == 0));
            } else {
                if (conds.size() == 2) {//是条件组
                    if (idx0 instanceof String) {
                        r = new StringBuilder((isfirst ? "" : " " + idx0) + " ( " + whereSQL((List<Object>) conds.get(1), cnt == 0) + " ) ");
                    }
                    return r.toString();
                }
            }
            if (conds.size() == 4) {//是条件
                if (idx0 instanceof String) {
                    return (isfirst ? "" : " " + idx0) + " " + conds.get(1) + " " + conds.get(2) + " " + sqlvalue(conds.get(3));
                }
            }
            cnt++;
        }
        return r.toString();
    }

    protected List<String> updateSQL() {
        List<String> sqlList = new ArrayList<>();
        String updateString = "";
        for (JSONObject _t : dataJSON) {//dataJSON可以包含多个jsonString,每一个jsonString代表一个操作
            for (Object _j : _t.keySet()) {//为每一个jsonString构造k-v insertsql
                updateString = updateString + _j.toString() + "=" + sqlvalue(_t.get(_j)) + ",";
            }
            sqlList.add("update " + getFullForm() + " set " + StringHelper.build(updateString).removeTrailingFrom().toString() + whereSQL());
        }
        return sqlList;
    }

    public JSONObject getAndDelete() {
        try {
            var rs = (JSONObject) (((JSONArray<?>) _findex(false)).get(0));
            _delete(false);
            return rs;
        } catch (Exception e) {
            return null;
        } finally {
            reinit();
        }
    }

    public boolean delete() {
        return _delete(false) > 0;
    }

    public long deleteAll() {
        return _delete(true);
    }

    protected long _delete(boolean isall) {
        long rs = 0;
        Statement smt;
        if (conditionJSON.size() > 0 || isall) {
            Connection conn = getNewConnection();
            try {
                smt = conn.createStatement();
                String sql = TransactSQLInjection("delete from " + getFullForm() + whereSQL() + (isall ? "" : " limit 1"));
                TransactSQLInjection(sql);
                rs = smt.executeUpdate(sql);
            } catch (Exception e) {
                nLogger.logInfo(e);
            } finally {
                reinit();
                _Close(conn);
            }
        }
        return rs;
    }

    public JSONObject getAndInc(String fieldName) {
        return getAndAdd(fieldName, 1);
    }

    public boolean inc(String fieldName) {
        return add(fieldName, 1);
    }

    public JSONObject getAndDec(String fieldName) {
        return getAndAdd(fieldName, -1);
    }

    public boolean dec(String fieldName) {
        return add(fieldName, -1);
    }

    public JSONObject getAndAdd(String fieldName, long num) {
        data("{\"" + fieldName + "\":\"" + fieldName + (num > 0 ? "+" : "-") + Math.abs(num) + "\"}");
        return getAndUpdate();
    }

    public boolean add(String fieldName, long num) {
        data("{\"" + fieldName + "\":\"" + fieldName + (num > 0 ? "+" : "-") + Math.abs(num) + "\"}");
        return update();
    }

    public JSONObject find() {
        limit(1);
        JSONArray fd = _find(false);
        return fd == null ? null : (fd.size() > 0 ? (JSONObject) fd.get(0) : null);
    }

    public JSONArray<JSONObject> select() {
        boolean skipMode = (skipNo > 0 && limitNo < 1);
        if (skipMode) {
            long fromCount = _count();
            if (fromCount > 0) {
                limit(Integer.parseInt(String.valueOf(fromCount)));
            }
        }
        return _find(true);
    }

    protected String limitSQl() {
        return limitNo > 0 ? " limit " + (skipNo > 0 ? skipNo + "," : "") + limitNo : "";
    }

    protected String sortSQL() {//只有第一个有效
        String rs = "";
        if (sortBSON.size() > 0) {
            for (Object item : sortBSON.keySet()) {
                rs = " order by " + item.toString() + " " + sortBSON.get(item).toString();
                break;
            }
        }
        return rs;
    }

    protected Object _findex(boolean isall) {
        //ResultSet rs;
        Object rs;
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            if (!isall) {
                if (limitNo == 0)
                    limitNo = 1;
            }
            String sql = TransactSQLInjection("select " + fieldSQL() + " from " + getFullForm() + whereSQL() + sortSQL() + limitSQl());
            TransactSQLInjection(sql);
            rs = col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            rs = null;
            nLogger.logInfo(e);
        }
        //这里关闭连接，结果集就没有了
        finally {
            _Close(conn);
        }
        return rs;
    }

    protected JSONArray _find(boolean isall) {//不支持groupby
        try {
            return (JSONArray) _findex(isall);
        } catch (Exception e) {
            nLogger.logInfo(e);
            // TODO Auto-generated catch block
            //nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return null;
    }

    public JSONArray<JSONObject> group() {
        return group(null);
    }

    public String getConditionString() {
        return conditionJSON.toString();
    }

    /**
     * @param groupName //groupby fieldName
     * @return
     */
    public JSONArray<JSONObject> group(String groupName) {
        String groupSQL;
        String sql;
        String _valueName = groupByField == null || groupByField.equals("") ? groupName : groupByField;
        String otherfield = "";
        if (_count)
            otherfield += ", count(" + _distinctfield(_valueName) + ") as count";
        if (_sum)
            otherfield += ", sum(" + _distinctfield(_valueName) + ") as total";
        if (_max)
            otherfield += ", max(" + _distinctfield(_valueName) + ") as max";
        if (_min)
            otherfield += ", min(" + _distinctfield(_valueName) + ") as min";
        if (_avg)
            otherfield += ", avg(" + _distinctfield(_valueName) + ") as avg";
        String condString = whereSQL();
        groupSQL = groupName == null || groupName.equals("") ? "" : (condString) + " group by " + groupName;
        sql = TransactSQLInjection("select " + fieldSQL() + otherfield + " from " + getFullForm() + groupSQL + sortSQL() + limitSQl());
        TransactSQLInjection(sql);
        JSONArray fd;
        Statement smt;
        Connection conn = getNewConnection();
        try {
            smt = conn.createStatement();
            fd = col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            fd = null;
            nLogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return fd;
    }

    protected String _distinctfield(String str) {
        String rs = str;
        if (_distinct) {
            rs = "DISTINCT(" + str + ")";
        }
        return rs;
    }

    public Sql distinct() {
        _distinct = true;
        return this;
    }

    public JSONArray<String> distinct(String fieldName) {
        boolean havefield = false;
        StringBuilder fieldString = new StringBuilder();
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            if (fieldVisible.size() > 0) {
                for (String item : fieldVisible) {
                    if (item.equals(fieldName)) {
                        fieldString.insert(0, "DISTINCT(" + item + "),");
                        havefield = true;
                    }
                    fieldString.append(item).append(",");
                }
            }
            if (!havefield) {
                fieldString.insert(0, "DISTINCT(" + fieldName + "),");
            }
            fieldString = new StringBuilder(StringHelper.build(fieldString.toString()).trimFrom(',').toString());
            String sql = TransactSQLInjection("select " + fieldString + " from " + getFullForm() + whereSQL() + sortSQL() + limitSQl());
            TransactSQLInjection(sql);
            return col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return null;
    }

    public JSONArray<JSONObject> page(int pageidx, int pagemax) {//普通分页
        return skip((pageidx - 1) * pagemax).limit(pagemax).select();
    }

    public JSONArray<JSONObject> page(int pageidx, int pagemax, Object lastId, String fastField) {//普通分页
        return skip((pageidx - 1) * pagemax).limit(pagemax).gt(fastField, lastId).select();
    }

    protected long _count() {
        //ResultSet rd;
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            String sql = TransactSQLInjection("select count(*) from " + getFullForm());
            TransactSQLInjection(sql);
            return (long) Result(smt.executeQuery(sql));
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            _Close(conn);
        }
        return 0;
    }

    public long count() {
        return count(false);
    }

    public long count(boolean islist) {
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            String sql = TransactSQLInjection("select count(*) from " + getFullForm() + whereSQL());
            //TransactSQLInjection(Sql);
            return (long) Result(smt.executeQuery(sql));
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            if (!islist) {
                reinit();
            }
            _Close(conn);
        }
        return 0;
    }

    public Sql count(String groupbyString) {//某字段分组后数量
        groupByField = groupbyString;
        _count = true;
        return this;
    }

    public Sql max(String groupbyString) {
        groupByField = groupbyString;
        _max = true;
        return this;
    }

    public Sql min(String groupbyString) {
        groupByField = groupbyString;
        _min = true;
        return this;
    }

    public Sql avg(String groupbyString) {
        groupByField = groupbyString;
        _avg = true;
        return this;
    }

    public Sql sum(String groupbyString) {
        groupByField = groupbyString;
        _sum = true;
        return this;
    }

    protected List<String> getResultCol(ResultSet rst) throws SQLException {
        ResultSetMetaData m = rst.getMetaData();//获取 列信息;
        int columns = m.getColumnCount();
        List<String> array = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            array.add(m.getColumnName(i));
        }
        return array;
    }

    protected List<String> col2list(ResultSet rst) {
        List<String> tableList = new ArrayList<>();
        try {
            if (rst.getMetaData().getColumnCount() > 0) {
                while (rst.next()) {
                    tableList.add(rst.getObject(1).toString());
                }
            }
        } catch (Exception e) {
            tableList.clear();
            nLogger.logInfo(e);
        }
        return tableList;
    }

    protected JSONArray col2jsonArray(ResultSet rst) {
        JSONArray rsj = new JSONArray();
        ResultSetMetaData m;
        int columns;

        //String aString;
        try {
            m = rst.getMetaData();//获取 列信息
            columns = m.getColumnCount();
            while (rst.next()) {
                JSONObject obj = new JSONObject();
                for (int i = 1; i <= columns; i++) {
                    // 过滤隐藏字段
                    String columnsName = m.getColumnName(i);
                    if (fieldDisable.contains(columnsName)) {
                        continue;
                    }
                    Object tobj = null;
                    try {
                        tobj = rst.getObject(i);
                    } catch (SQLException sqle) {
                        if (sqle.getSQLState().equals("S1009")) {
                            tobj = 0;
                        }
                    }
                    // 时间日期转换成时间戳
                    /*
                    if (tobj instanceof Timestamp) {
                        tobj = TimeHelper.build().dateTimeToTimestamp(tobj.toString());
                    }
                    */
                    if (tobj instanceof Timestamp ti) {
                        tobj = ti.toString();
                    }
                    obj.put(columnsName, tobj);
                }
                rsj.add(obj);
            }
        } catch (Exception e) {
            rsj = null;
            nLogger.logInfo(e);
        }
        return rsj;
    }

    protected Object Result(ResultSet rst) {
        Object rs = null;
        try {
            if (rst.next()) {
                rs = rst.getObject(1);
            }
        } catch (Exception e) {
        }
        return rs;
    }

    protected Object Result(ResultSet rst, String fieldName) {
        Object rs = null;
        try {
            if (rst.next()) {
                rs = rst.getObject(fieldName);
            }
        } catch (Exception e) {
        }
        return rs;
    }

    public String getFormName() {
        return formName;
    }

    /*
    protected String sqlvalue(Object _value) {//未考虑json字符串
        String rValue;
        String value;
        try {
            value = _value.toString();
            String[] _values = value.split(":");
            if (_values[0].equalsIgnoreCase("func")) {
                rValue = StringHelper.join(_values, ":", 1, -1);
            } else {
                rValue = StringHelper.typeString(_value);
            }
            JSONObject jsontest = JSONObject.toJSON(rValue);
            if (jsontest != null) {
                rValue = "'" + rValue + "'";
            }
        } catch (Exception e) {
            rValue = StringHelper.typeString(_value);
        }
        return rValue;
    }
    */

    protected String sqlvalue(Object _value) {//未考虑json字符串
        String rValue;
        String value;
        try {
            value = _value.toString();
            String[] _values = value.split(":");
            if (_values[0].equalsIgnoreCase("func")) {
                rValue = StringHelper.join(_values, ":", 1, -1);
            } else {
                rValue = StringHelper.typeString(_value, "'");
            }
            JSONObject jsontest = JSONObject.toJSON(rValue);
            if (jsontest != null) {
                rValue = "'" + rValue + "'";
            }
        } catch (Exception e) {
            rValue = StringHelper.typeString(_value, "'");
        }
        return rValue;
    }

    public Sql bind(String ownerID) {
        ownId = ownerID == null ? "" : ownerID;
        return this;
    }

    public int limit() {
        return limitNo;
    }

    public int pageMax(int max) {
        double c = count(true);
        double d = c / max;
        return (int) Math.ceil(d);
    }

    public Sql dirty() {
        isDirty = true;
        return this;
    }

    public void clear() {
        isDirty = false;
        reinit();
    }

    //获得全部表
    public String[] getAllTables() {
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            return col2list(
                    smt.executeQuery(
                            TransactSQLInjection("show tables")))
                    .toArray(new String[0]);
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
            _Close(conn);
        }
        return new String[0];
    }

    public boolean run(String cmd) {
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            TransactSQLInjection(cmd);
            return smt.execute(cmd);
        } catch (Exception e) {
            nLogger.logInfo(e);
            return false;
        }
        //这里关闭连接，结果集就没有了
        finally {
            _Close(conn);
        }
    }

    public static class sqlmethod {
        public final static int insert = 1;
        public final static int update = 2;
        public final static int delete = 3;
    }

    public String func(String str) {
        return "func:" + str + " ";
    }

    /**
     * 10位unixtime
     */
    public String now() {
        return func("now()");
    }

    /**
     * 10位unixtime
     */
    public String formUnixTime(long unixTime) {
        return func("from_unixtime(" + unixTime + ")");
    }

    public String curDate() {
        return func("CURDATE()");
    }

    public String curTime() {
        return func("CURTIME()");
    }
}
