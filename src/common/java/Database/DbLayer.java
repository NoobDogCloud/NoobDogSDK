package common.java.Database;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Cache.Cache;
import common.java.Config.Config;
import common.java.HttpServer.HttpContext;
import common.java.Reflect._reflect;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;


public class DbLayer implements InterfaceDatabase<DbLayer> {
    private final HashMap<String, List<Function<Object, Object>>> outHookFunc = new HashMap<>();
    private final HashMap<String, List<Function<Object, Object>>> inHookFunc = new HashMap<>();
    public int _dbName;
    public String formName;
    private _reflect _db;            //数据库抽象对象
    private Cache cache;        //缓存抽象对象
    private String ownId;
    private boolean out_piper_flag = true;

    private DbLayer() {
        init(null);
    }

    private DbLayer(String configName) {
        init(configName);
    }

    private DbLayer(boolean notLoad) {
        formName = "";
    }

    public static DbLayer buildWithConfig(String configContent) {
        DbLayer db = new DbLayer(true);
        db.getDbByConfigContent(configContent);
        return db;
    }

    public static DbLayer build() {
        return new DbLayer();
    }

    public static DbLayer build(String configName) {
        return new DbLayer(configName);
    }

    public DbLayer setPiperEnable(boolean flag) {
        out_piper_flag = flag;
        return this;
    }

    /**
     * 自动生成多OR条件
     */
    public DbLayer putAllOr(String ids) {
        return putAllOr(ids, getGeneratedKeys());
    }

    public DbLayer putAllOr(String ids, String field) {
        DbFilter dbf = DbFilter.buildDbFilter();
        if (!StringHelper.isInvalided(ids)) {
            String[] idList = ids.split(",");
            if (idList.length > 0) {
                for (String s : idList) {
                    dbf.or().eq(field, s);
                }
                groupCondition(dbf.buildEx());
            }
        }
        return this;
    }

    public DbLayer addFieldOutPipe(String fieldName, Function<Object, Object> func) {
        return getDbLayer(fieldName, func, outHookFunc);
    }

    public DbLayer addFieldInPipe(String fieldName, Function<Object, Object> func) {
        return getDbLayer(fieldName, func, inHookFunc);
    }

    private DbLayer getDbLayer(String fieldName, Function<Object, Object> func, HashMap<String, List<Function<Object, Object>>> inHookFunc) {
        if (func != null) {
            List<Function<Object, Object>> link = inHookFunc.get(fieldName);
            if (link == null) {
                link = new ArrayList<>();
            }
            link.add(func);
            inHookFunc.put(fieldName, link);
        }
        return this;
    }

    private void fieldPiper(JSONObject data, HashMap<String, List<Function<Object, Object>>> inList) {
        if (JSONObject.isInvalided(data)) {
            return;
        }
        for (String k : inList.keySet()) {
            if (data.containsKey(k)) {
                Object outVal = data.get(k);
                List<Function<Object, Object>> link = inList.get(k);
                for (Function<Object, Object> func : link) {
                    outVal = func.apply(outVal);
                }
                data.put(k, outVal);
            }
        }
    }

    private Object fieldOutPiper(Object data) {
        if (data == null) {
            return null;
        }
        if (out_piper_flag) {
            if (data instanceof JSONArray) {
                for (Object item : (JSONArray) data) {
                    fieldPiper((JSONObject) item, outHookFunc);
                }
            } else if (data instanceof JSONObject) {
                fieldPiper((JSONObject) data, outHookFunc);
            }
        }
        return data;
    }


    public _reflect getDBObject(String cN) {
        String _configString = Config.netConfig(cN);
        try {
            if (_configString != null) {
                _db = getDbByConfigContent(_configString);
            } else {
                nLogger.logInfo("DB配置信息[" + cN + "]为空:=>" + null);
            }
            _db.privateMode();//内部调用，启动私有模式
        } catch (Exception e) {
            nLogger.logInfo(e, "连接关系型数据系统失败! 配置名:[" + cN + "]");
            _db = null;
        }
        return _db;
    }

    public _reflect getDbByConfigContent(String _configString) {
        JSONObject obj = JSONObject.toJSON(_configString);
        if (obj != null) {
            String dbName = obj.getString("dbName").toLowerCase();
            switch (dbName) {
                case "mongodb": {
                    _db = (new _reflect(Mongodb.class)).newInstance(_configString);
                    _dbName = dbType.mongodb;
                    break;
                }
                case "oracle": {
                    _db = (new _reflect(Oracle.class)).newInstance(_configString);
                    _dbName = dbType.oracle;
                    break;
                }
                default: {
                    _db = (new _reflect(Sql.class)).newInstance(_configString);
                    _dbName = dbType.mysql;
                }
            }
        } else {
            nLogger.logInfo("DB配置信息格式错误 ：" + _configString);
        }
        return _db;
    }

    private Cache getCache() {
        if (cache == null) {
            try {
                String cacheConfigName = null;
                if (MicroServiceContext.current().hasData()) {
                    cacheConfigName = MicroServiceContext.current().config().cache();
                } else if (AppContext.current().hasData()) {
                    cacheConfigName = AppContext.current().config().cache();
                }
                cache = cacheConfigName != null ? Cache.getInstance(cacheConfigName) : null;
            } catch (Exception e) {
                cache = null;
                nLogger.logInfo(e, "数据系统绑定的缓存系统初始化失败");
            }
        }
        return cache;
    }

    private void init(String inputConfigName) {
        try {
            String configName = null;
            if (inputConfigName == null) {
                if (MicroServiceContext.current().hasData()) {
                    configName = MicroServiceContext.current().config().db();
                } else if (AppContext.current().hasData()) {
                    configName = AppContext.current().config().db();
                }
            } else {
                configName = inputConfigName;
            }
            if (configName == null || configName.equals("")) {
                throw new RuntimeException("数据库配置丢失");
            }
            _db = getDBObject(configName);

        } catch (Exception e) {
            // TODO: handle exception
            nLogger.logInfo(e, "DB配置读取失败");
        }
    }

    /**
     * 从缓存取数据，如果缓存不存在数据，那么从数据库取并填充
     */
    public JSONArray selectByCache() {
        return selectByCache(3);
    }

    /**
     * 从缓存取数据，如果缓存不存在数据，那么从数据库取并填充
     */
    public JSONArray selectByCache(int second) {
        JSONArray rs = null;
        String key = getFormName() + getConditionString();
        Cache c = getCache();
        if (c != null) {
            rs = c.getJsonArray(key);
        }
        if (rs == null) {//不存在
            rs = select();
            if (rs != null && c != null) {
                if (rs.size() > 0) {
                    c.set(key, second, rs.toString());
                }
            }
        }
        return rs;
    }

    public void invalidCache() {
        String key = getFormName() + getConditionString();
        Cache c = getCache();
        if (c != null) {
            c.delete(key);
        }
    }

    public JSONObject findByCache(int second) {
        JSONObject rs = null;
        String key = getFormName() + getConditionString();
        Cache c = getCache();
        if (c != null) {
            rs = c.getJson(key);
        }
        if (rs == null) {//不存在
            rs = this.find();
            if (rs != null && c != null) {
                if (rs.size() > 0) {
                    c.set(key, second, rs.toString());
                }
            }
        }
        return rs;
    }

    public JSONObject findByCache() {
        return findByCache(3);
    }

    private void updateFix() {
        form(formName);
        bind(ownId);
    }

    //---------------------------db接口引用
    public void Close() {
        //_db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName, CondValue);
    }

    public DbLayer and() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public DbLayer or() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public boolean nullCondition() {
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer where(JSONArray condArray) {
        if (condArray == null) {
            condArray = new JSONArray();
        }
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), condArray);
        return this;
    }

    public DbLayer groupCondition(List<List<Object>> conds) {
        if (conds == null) {
            conds = new ArrayList<>();
        }
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), conds);
        // System.out.println(getCond());
        return this;
    }

    public DbLayer groupWhere(JSONArray conds) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), conds);
        // System.out.println(getCond());
        return this;
    }

    public DbLayer eq(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer ne(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer gt(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer lt(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer gte(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer lte(String field, Object value) {//One Condition
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer like(String field, Object value) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field, value);
        return this;
    }

    public DbLayer data(String jsonString) {//One Condition
        return data(JSONObject.toJSON(jsonString));
    }

    public DbLayer data(JSONObject doc) {//One Condition
        fieldPiper(doc, inHookFunc);
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), doc);
        return this;
    }

    public List<JSONObject> clearData() {
        return (List<JSONObject>) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public List<JSONObject> data() {
        return (List<JSONObject>) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer field() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public DbLayer field(String[] fieldString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldString);
        return this;
    }

    public DbLayer mask(String[] fieldString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldString);
        return this;
    }

    public DbLayer form(String _formName) {
        formName = _formName;
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), _formName);
        return this;
    }

    public DbLayer skip(int no) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), no);
        return this;
    }

    public DbLayer limit(int no) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), no);
        return this;
    }

    public DbLayer asc(String field) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field);
        return this;
    }

    public DbLayer desc(String field) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), field);
        return this;
    }

    public DbLayer findOne() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public List<Object> insert() {
        updateFix();
        return (List<Object>) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONObject getAndUpdate() {
        updateFix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public boolean update() {
        updateFix();
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public long updateAll() {
        updateFix();
        return (long) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONObject getAndDelete() {
        updateFix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public boolean delete() {
        updateFix();
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public long deleteAll() {
        updateFix();
        return (long) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONObject getAndInc(String fieldName) {
        updateFix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public boolean inc(String fieldName) {
        updateFix();
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public JSONObject getAndDec(String fieldName) {
        updateFix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public boolean dec(String fieldName) {
        updateFix();
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public JSONObject getAndAdd(String fieldName, long num) {
        updateFix();
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName, num);
    }

    public boolean add(String fieldName, long num) {
        updateFix();
        return (boolean) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName, num);
    }

    public JSONObject getAndSub(String fieldName, long num) {
        return getAndAdd(fieldName, -1 * num);
    }

    public boolean sub(String fieldName, long num) {
        return add(fieldName, -1 * num);
    }

    public JSONObject find() {
        updateFix();
        return (JSONObject) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName()));
    }

    public JSONArray select() {
        updateFix();
        return (JSONArray) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName()));
    }

    public String getConditionString() {
        updateFix();
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONArray group() {
        updateFix();
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONArray group(String groupName) {
        updateFix();
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupName);
    }

    public JSONArray distinct(String fieldName) {
        updateFix();
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), fieldName);
    }

    public JSONArray page(int pageidx, int pagemax) {
        updateFix();
        return (JSONArray) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), pageidx, pagemax));
    }

    public final JSONArray page(int pageidx, int pagemax, int lastid, String fastfield) {
        updateFix();
        return (JSONArray) fieldOutPiper(_db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), pageidx, pagemax, lastid, fastfield));
    }

    public long count() {
        return (long) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer count(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer max(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer min(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer avg(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public DbLayer sum(String groupbyString) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), groupbyString);
        return this;
    }

    public String getFormName() {
        return formName;
        //return (String)_db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public String getForm() {
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public String getFullForm() {
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public void asyncInsert() {
        updateFix();
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public Object insertOnce() {
        updateFix();
        return _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer bind(String ownerID) {
        ownId = ownerID == null || ownerID.equals("0") ? "" : ownerID;
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), ownId);
        return this;
    }

    public DbLayer bind() {
        int appId = HttpContext.current().appId();
        if (appId != 0) {
            try {
                ownId = StringHelper.toString(appId);
                bind(ownId);
            } catch (Exception e) {
                nLogger.logInfo(e, "应用ID不合法");
            }
        }
        return this;
    }

    public int limit() {
        return (int) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public int pageMax(int max) {
        return (int) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), max);
    }

    public String getGeneratedKeys() {
        return (String) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer dirty() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
        return this;
    }

    public void clear() {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max) {
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), func, max);
    }

    public JSONArray scan(Function<JSONArray, JSONArray> func, int max, int synNo) {
        return (JSONArray) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), func, max, synNo);
    }

    public JSONObject getCond() {
        return (JSONObject) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public DbLayer setCond(JSONObject conJSON) {
        _db._call(Thread.currentThread().getStackTrace()[1].getMethodName(), conJSON);
        return this;
    }

    public List<String> getAllTables() {
        return (List<String>) _db._call(Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    public static class dbType {
        public final static int mongodb = 1;
        public final static int mysql = 2;
        public final static int oracle = 3;
    }
}