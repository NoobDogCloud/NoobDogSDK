package common.java.Database;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Cache.Cache;
import common.java.Config.Config;
import common.java.Http.Server.HttpContext;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;


public class DbLayer implements IDBLayer<DbLayer> {
    private final HashMap<String, List<Function<Object, Object>>> outHookFunc = new HashMap<>();
    private final HashMap<String, List<Function<Object, Object>>> inHookFunc = new HashMap<>();
    public int _dbName;
    public String formName;
    private IDBLayer _db;            //数据库抽象对象
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


    public IDBLayer getDBObject(String cN) {
        String _configString = Config.netConfig(cN);
        try {
            if (_configString != null) {
                _db = getDbByConfigContent(_configString);
            } else {
                nLogger.logInfo("DB配置信息[" + cN + "]为空:=>" + null);
            }
        } catch (Exception e) {
            nLogger.logInfo(e, "连接关系型数据系统失败! 配置名:[" + cN + "]");
            _db = null;
        }
        return _db;
    }

    public IDBLayer getDbByConfigContent(String _configString) {
        JSONObject obj = JSONObject.toJSON(_configString);
        if (obj != null) {
            String dbName = obj.getString("dbName").toLowerCase();
            switch (dbName) {
                case "mongodb" -> {
                    _db = new Mongodb(_configString);
                    _dbName = dbType.mongodb;
                    // break;
                }
                case "oracle" -> {
                    _db = new Oracle(_configString);
                    _dbName = dbType.oracle;
                    // break;
                }
                case "h2" -> {
                    _db = new H2(_configString);
                    _dbName = dbType.h2;
                    // break;
                }
                default -> {
                    _db = new Sql(_configString);
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
    public JSONArray<JSONObject> selectByCache() {
        return selectByCache(3);
    }

    /**
     * 从缓存取数据，如果缓存不存在数据，那么从数据库取并填充
     */
    public JSONArray<JSONObject> selectByCache(int second) {
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

    public void addConstantCond(String fieldName, Object CondValue) {
        _db.addConstantCond(fieldName, CondValue);
    }

    public DbLayer and() {
        _db.and();
        return this;
    }

    public DbLayer or() {
        _db.or();
        return this;
    }

    public boolean nullCondition() {
        return _db.nullCondition();
    }

    public DbLayer where(JSONArray condArray) {
        if (condArray == null) {
            condArray = new JSONArray();
        }
        _db.where(condArray);
        return this;
    }

    public DbLayer groupCondition(List<List<Object>> conds) {
        if (conds == null) {
            conds = new ArrayList<>();
        }
        _db.groupCondition(conds);
        return this;
    }

    public DbLayer groupWhere(JSONArray<JSONObject> conds) {
        _db.groupWhere(conds);
        return this;
    }

    public DbLayer eq(String field, Object value) {//One Condition
        _db.eq(field, value);
        return this;
    }

    public DbLayer ne(String field, Object value) {//One Condition
        _db.ne(field, value);
        return this;
    }

    public DbLayer gt(String field, Object value) {//One Condition
        _db.gt(field, value);
        return this;
    }

    public DbLayer lt(String field, Object value) {//One Condition
        _db.lt(field, value);
        return this;
    }

    public DbLayer gte(String field, Object value) {//One Condition
        _db.gte(field, value);
        return this;
    }

    public DbLayer lte(String field, Object value) {//One Condition
        _db.lte(field, value);
        return this;
    }

    public DbLayer like(String field, Object value) {
        _db.like(field, value);
        return this;
    }

    public DbLayer data(String jsonString) {//One Condition
        return data(JSONObject.toJSON(jsonString));
    }

    public DbLayer data(JSONObject doc) {//One Condition
        fieldPiper(doc, inHookFunc);
        _db.data(doc);
        return this;
    }

    public List<JSONObject> data() {
        return _db.data();
    }

    public DbLayer field() {
        _db.field();
        return this;
    }

    public DbLayer field(String[] fieldString) {
        _db.field(fieldString);
        return this;
    }

    public DbLayer mask(String[] fieldString) {
        _db.mask(fieldString);
        return this;
    }

    public DbLayer form(String _formName) {
        formName = _formName;
        _db.form(_formName);
        return this;
    }

    public DbLayer skip(int no) {
        _db.skip(no);
        return this;
    }

    public DbLayer limit(int no) {
        _db.limit(no);
        return this;
    }

    public DbLayer asc(String field) {
        _db.asc(field);
        return this;
    }

    public DbLayer desc(String field) {
        _db.desc(field);
        return this;
    }

    public List<Object> insert() {
        updateFix();
        return _db.insert();
    }

    public JSONObject getAndUpdate() {
        updateFix();
        return _db.getAndUpdate();
    }

    public boolean update() {
        updateFix();
        return _db.update();
    }

    public long updateAll() {
        updateFix();
        return _db.updateAll();
    }

    public JSONObject getAndDelete() {
        updateFix();
        return _db.getAndDelete();
    }

    public boolean delete() {
        updateFix();
        return _db.delete();
    }

    public long deleteAll() {
        updateFix();
        return _db.deleteAll();
    }

    public JSONObject getAndInc(String fieldName) {
        updateFix();
        return _db.getAndInc(fieldName);
    }

    public boolean inc(String fieldName) {
        updateFix();
        return _db.inc(fieldName);
    }

    public JSONObject getAndDec(String fieldName) {
        updateFix();
        return _db.getAndDec(fieldName);
    }

    public boolean dec(String fieldName) {
        updateFix();
        return _db.dec(fieldName);
    }

    public JSONObject getAndAdd(String fieldName, long num) {
        updateFix();
        return _db.getAndAdd(fieldName, num);
    }

    public boolean add(String fieldName, long num) {
        updateFix();
        return _db.add(fieldName, num);
    }

    public JSONObject getAndSub(String fieldName, long num) {
        return getAndAdd(fieldName, -1 * num);
    }

    public boolean sub(String fieldName, long num) {
        return add(fieldName, -1 * num);
    }

    public JSONObject find() {
        updateFix();
        return (JSONObject) fieldOutPiper(_db.find());
    }

    public JSONArray<JSONObject> select() {
        updateFix();
        return (JSONArray<JSONObject>) fieldOutPiper(_db.select());
    }

    public String getConditionString() {
        updateFix();
        return _db.getConditionString();
    }

    public JSONArray<JSONObject> group() {
        updateFix();
        return _db.group();
    }

    public JSONArray<JSONObject> group(String groupName) {
        updateFix();
        return _db.group(groupName);
    }

    public JSONArray<String> distinct(String fieldName) {
        updateFix();
        return _db.distinct(fieldName);
    }

    public JSONArray<JSONObject> page(int pageidx, int pagemax) {
        updateFix();
        return (JSONArray<JSONObject>) fieldOutPiper(_db.page(pageidx, pagemax));
    }

    public JSONArray<JSONObject> page(int pageidx, int pagemax, Object lastid, String fastfield) {
        updateFix();
        return (JSONArray<JSONObject>) fieldOutPiper(_db.page(pageidx, pagemax, lastid, fastfield));
    }

    public long count() {
        return _db.count();
    }

    public DbLayer count(String groupbyString) {
        _db.count(groupbyString);
        return this;
    }

    public DbLayer max(String groupbyString) {
        _db.max(groupbyString);
        return this;
    }

    public DbLayer min(String groupbyString) {
        _db.min(groupbyString);
        return this;
    }

    public DbLayer avg(String groupbyString) {
        _db.avg(groupbyString);
        return this;
    }

    public DbLayer sum(String groupbyString) {
        _db.sum(groupbyString);
        return this;
    }

    public String getFormName() {
        return formName;
        //return_db._call();
    }

    public String getForm() {
        return _db.getForm();
    }

    public String getFullForm() {
        return _db.getFullForm();
    }

    public void asyncInsert() {
        updateFix();
        _db.asyncInsert();
    }

    public Object insertOnce() {
        updateFix();
        return _db.insertOnce();
    }

    public DbLayer bind(String ownerID) {
        ownId = ownerID == null || ownerID.equals("0") ? "" : ownerID;
        _db.bind(ownId);
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
        return _db.limit();
    }

    public int pageMax(int max) {
        return _db.pageMax(max);
    }

    public String getGeneratedKeys() {
        return _db.getGeneratedKeys();
    }

    public DbLayer dirty() {
        _db.dirty();
        return this;
    }

    public void clear() {
        _db.clear();
    }

    public List<JSONObject> clearData() {
        return _db.clearData();
    }

    public JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max) {
        return _db.scan(func, max);
    }

    public JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max, int synNo) {
        return _db.scan(func, max, synNo);
    }

    public List<List<Object>> getCond() {
        return _db.getCond();
    }

    public DbLayer setCond(List<List<Object>> conJSON) {
        _db.setCond(conJSON);
        return this;
    }

    public List<String> getAllTables() {
        return _db.getAllTables();
    }

    public boolean run(String cmd) {
        return _db.run(cmd);
    }

    public String func(String str) {
        return _db.func(str);
    }

    /**
     * 10位unixtime
     */
    public String now() {
        return _db.now();
    }

    /**
     * 10位unixtime
     */
    public String formUnixTime(long unixTime) {
        return _db.formUnixTime(unixTime);
    }

    public String curDate() {
        return _db.curDate();
    }

    public String curTime() {
        return _db.curTime();
    }

    public static class dbType {
        public final static int mongodb = 1;
        public final static int mysql = 2;
        public final static int oracle = 3;
        public final static int h2 = 4;
    }

}