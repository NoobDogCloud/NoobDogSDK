package common.java.Database;

import com.mongodb.MongoClient;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.json.StrictJsonWriter;
import org.bson.types.ObjectId;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * {
 * "keepalive": true,
 * "dbName": "Mongodb",
 * "user": "",
 * "password": "",
 * "database": "test",
 * "replicaSet": "repset",
 * "nodeAddresses": ["123.57.213.15:27017", "123.57.213.15:27018", "123.57.213.15:27019"]
 * }
 */
public class Mongodb implements IDBLayer<Mongodb> {
    private static final HashMap<String, MongoClient> DataSource;
    private static final JsonWriterSettings build;

    static {
        build = JsonWriterSettings.builder()
                .outputMode(JsonMode.EXTENDED)
                .int64Converter((Long value, StrictJsonWriter writer) -> writer.writeNumber(Long.toString(value)))
                .int32Converter((Integer value, StrictJsonWriter writer) -> writer.writeNumber(Integer.toString(value)))
                .objectIdConverter((ObjectId value, StrictJsonWriter writer) -> writer.writeString(value.toString()))
                .build();
        DataSource = new HashMap<>();
        java.util.logging.Logger.getLogger("org.Mongodb.driver").setLevel(Level.SEVERE);
    }

    private MongoDatabase mongoDatabase;
    private String _configString;
    //private static Mongodb Mongodb;
    private MongoClient mongoClient;
    /*
     * 查询相关代码
     * {
     * 	[
     * 		["and","key","eq","value"],
     * 		["and","key","eq","value"],
     * 		["and","key","eq","value"]
     * 		...
     *  ]
     *
     * }
     */
    private boolean conditiobLogicAnd;
    private List<List<Object>> conditionJSON;    //条件
    private String formName;
    private String groupbyfield;
    private int skipNo;
    private int limitNo;
    private MongoCollection<Document> collection;
    private BasicDBObject fieldBSON;
    private Set<String> fieldVisible;       // 可见字段
    private Set<String> fieldDisable;       // 不可见字段
    private List<Bson> sortBSON;
    private List<Document> dataBSON;
    private List<Bson> updateBSON;
    private boolean _count;
    private boolean _max;
    private boolean _min;
    private boolean _sum;
    private boolean _avg;
    private boolean _distinct;
    // private boolean _atom;
    private String ownid;
    private boolean isDirty;
    private HashMap<String, Object> constantConds;
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    public Mongodb(String configString) {
        _configString = configString;
        constantConds = new HashMap<>();
        initMongodb();
        reinit();
    }

    Mongodb() {
        isDirty = false;
        formName = "";
        reinit();
    }

    public static JSONObject bson2json(Document _bson) {
        return _bson != null ? JSONObject.toJSON(_bson.toJson(build)) : new JSONObject();
    }

    private void initMongodb() {
        try {
            //DebugPerformance dp = new DebugPerformance();
            mongodbConfig mongodbConfig = new mongodbConfig();
            MongoClientURI mgoURI = mongodbConfig.json2mongouri(_configString);
            if (DataSource.containsKey(_configString)) {
                mongoClient = DataSource.get(_configString);
            } else {
                //MongoClientOptions.Builder build = new MongoClientOptions.Builder();
                synchronized (this) {
                    try {
                        mongoClient = new MongoClient(mgoURI);
                        ReadPreference.primaryPreferred();
                        DataSource.put(_configString, mongoClient);
                    } catch (Exception e) {
                        if (mongoClient != null) {
                            mongoClient.close();
                        }
                        nLogger.logInfo(e);
                    }
                }
            }
            mongoDatabase = mongoClient.getDatabase(mongodbConfig.database);
            //dp.end();
        } catch (Exception e) {
            if (mongoClient != null) {
                mongoClient.close();
            }
            nLogger.logInfo(e);
        }
    }

    public Mongodb data(JSONObject doc) {
        dataBSON.add(json2document(doc));
        return this;
    }

    public Mongodb data(String jsonString) {
        data(JSONObject.toJSON(jsonString));
        return this;
    }

    public List<JSONObject> clearData() {
        List<JSONObject> v = data();
        dataBSON.clear();
        return v;
    }

    public List<JSONObject> data() {
        List<JSONObject> arr = new ArrayList<>();
        for (var doc : dataBSON) {
            arr.add(bson2json(doc));
        }
        return arr;
    }

    private void reinit() {
        if (isDirty) {//脏执行下不重置
            isDirty = false;
            return;
        }
        //tempCondtion = new ArrayList<>();
        if (conditionJSON == null) {
            conditionJSON = new ArrayList<>();
        } else {
            conditionJSON.clear();
        }
        conditiobLogicAnd = true;

        if (fieldBSON == null) {
            fieldBSON = new BasicDBObject();
        } else {
            fieldBSON.clear();
        }

        if (fieldVisible == null) {
            fieldVisible = new HashSet<>();
        } else {
            fieldDisable.clear();
        }

        if (fieldDisable == null) {
            fieldDisable = new HashSet<>();
        } else {
            fieldDisable.clear();
        }

        if (sortBSON == null) {
            sortBSON = new ArrayList<>();
        } else {
            sortBSON.clear();
        }
        if (dataBSON == null) {
            dataBSON = new ArrayList<>();
        } else {
            dataBSON.clear();
        }
        if (updateBSON == null) {
            updateBSON = new ArrayList<>();
        } else {
            updateBSON.clear();
        }

        limitNo = 0;
        skipNo = 0;

        _count = false;
        _max = false;
        _min = false;
        _sum = false;
        _avg = false;

        _distinct = false;

        groupbyfield = "";

        ownid = null;


        and();

        for (String _key : constantConds.keySet()) {//补充条件
            eq(_key, constantConds.get(_key));
        }
    }

    public void addConstantCond(String fieldName, Object CondValue) {
        and();
        constantConds.put(fieldName, CondValue);
        eq(fieldName, CondValue);//载入的时候填入条件
    }

    public Mongodb and() {
        conditiobLogicAnd = true;
        return this;
    }

    public Mongodb or() {
        conditiobLogicAnd = false;
        return this;
    }

    private Object _getID(String field, Object value) {
        Object rvalue = value;
        if (field != null && field.equalsIgnoreCase("_id")) {
            if (value instanceof String) {
                rvalue = new ObjectId(value.toString());
            }
            if (value instanceof JSONObject) {
                rvalue = new ObjectId(((JSONObject) value).getString("$oid"));
            }
        }
        return rvalue;
    }

    /**
     * 判断条件是否为空
     *
     * @return
     */
    public boolean nullCondition() {
        return conditionJSON.size() == 0;
    }

    public Mongodb where(List<List<Object>> condArray) {
        conditionJSON.addAll(condArray);
        return this;
    }
    //逻辑变化的时候
    //

    /**
     * 条件组 field,logic,value
     *
     * @param condArray
     * @return
     */
    public Mongodb where(JSONArray<JSONObject> condArray) {
        JSONObject tmpJSON;
        String field, logic, link_login;
        Object value;
        if (condArray == null) {
            return null;
        }

        if (condArray.size() > 0) {
            for (JSONObject jObject : condArray) {
                field = null;
                logic = null;
                value = null;
                link_login = null;
                tmpJSON = jObject;
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

    private String logic2mongodb(String logic) {
        return switch (logic) {
            case "=", "==" -> "$eq";
            case "!=" -> "$ne";
            case ">" -> "$gt";
            case "<" -> "$lt";
            case ">=" -> "$gte";
            case "<=" -> "$lte";
            case "like" -> "$regex";
            default -> logic;
        };
    }

    private void addCondition(String field, Object value, String logic) {
        addCondition(field, value, logic, conditiobLogicAnd);
    }

    private void addCondition(String field, Object value, String logic, boolean link_logic) {
        // fixCondObject();
        // BasicDBList logicBSON;
        //JSONObject tempObject;
        String logicStr;
        if (value != null) {
            value = _getID(field, value);
            logicStr = logic2mongodb(logic);
            if (logicStr.equalsIgnoreCase("$regex")) {
                value = "^.*" + value.toString() + ".*$";
            }
            List<Object> bit = new ArrayList<>();

            // bit.add(conditiobLogicAnd ? "and" : "or");
            bit.add(link_logic ? "and" : "or");
            bit.add(field);
            bit.add(logicStr);
            bit.add(value);

            conditionJSON.add(bit);
            //near,in,nin,nor,not,text
        }
    }

    public Mongodb eq(String field, Object value) {//One Condition
        addCondition(field, value, "=");
        return this;
    }

    public Mongodb ne(String field, Object value) {//One Condition

        addCondition(field, value, "!=");
        return this;
    }

    public Mongodb gt(String field, Object value) {//One Condition

        addCondition(field, value, ">");
        return this;
    }

    public Mongodb lt(String field, Object value) {//One Condition

        addCondition(field, value, "<");
        return this;
    }

    public Mongodb gte(String field, Object value) {//One Condition

        addCondition(field, value, ">=");
        return this;
    }

    public Mongodb lte(String field, Object value) {//One Condition

        addCondition(field, value, "<=");
        return this;
    }

    public Mongodb like(String field, Object value) {

        //Pattern _value = Pattern.compile("^.*" + value.toString()+ ".*$", Pattern.CASE_INSENSITIVE);
        //这里根据value转换一下标识

        addCondition(field, value, "like");
        //addCondition(field,"^.*" + value.toString()+ ".*$","like");
        return this;
    }

    public Mongodb field() {
        fieldBSON.clear();
        fieldVisible.clear();
        fieldDisable.clear();
        return this;
    }

    public Mongodb field(String fieldString) {
        String[] fieldList = fieldString.split(",");
        return fieldList.length <= 1 ? null : field(fieldList);
    }

    public Mongodb mask(String fieldString) {
        String[] fieldList = fieldString != null ? fieldString.split(",") : null;
        return fieldList == null ? null : mask(fieldList);
    }

    public Mongodb field(String[] fieldList) {
        fieldVisible.addAll(Arrays.asList(fieldList));
        return this;
    }

    public Mongodb mask(String[] fieldList) {
        fieldDisable.addAll(Arrays.asList(fieldList));
        return this;
    }

    private void buildFieldBSON(Set<String> set) {
        for (String v : set) {
            fieldBSON.put(v, set);
        }
    }

    private BasicDBObject reBuildField() {
        // 选择字段多的为基准
        if (fieldVisible.size() > fieldDisable.size()) { // 可见字段 多于 不可见字段,去掉可见字段里不可见字段
            for (String v : fieldDisable) {
                fieldVisible.remove(v);
            }
            buildFieldBSON(fieldVisible);
        } else { // 不可见字段 多于 可见字段,去掉不可见字段里可见字段
            for (String v : fieldVisible) {
                fieldDisable.remove(v);
            }
            buildFieldBSON(fieldDisable);
        }
        return fieldBSON;
    }

    public Mongodb form(String _formName) {
        formName = _formName;
        collection = mongoDatabase.getCollection(getFullForm());
        return this;
    }

    public Mongodb skip(int no) {
        skipNo = no;
        return this;
    }

    public String getFullForm() {
        return ownid == null || ownid.equals("") ? formName : formName + "_" + ownid;
    }

    public String getForm() {
        return formName;
    }

    public Mongodb limit(int no) {
        limitNo = no;
        return this;
    }

    public Mongodb asc(String field) {
        sortBSON.add(Sorts.ascending(field));
        return this;
    }

    public Mongodb desc(String field) {
        sortBSON.add(Sorts.descending(field));
        return this;
    }

    public List<Document> clearDocument(List<Document> imp) {
        for (Document doc : imp) {
            doc.remove("_id");
        }
        return imp;
    }

    public List<Object> insert() {
        List<Object> rList = new ArrayList<>();
        asyncInsert();
        reinit();
        // int l = dataBSON.size();
        for (Document document : dataBSON) {
            rList.add(document.get("_id"));
        }
        return rList;
    }

    public Object insertOnce() {
        return _insertOnce(true);
    }

    public void asyncInsert() {
        dataBSON = clearDocument(dataBSON);
        if (dataBSON.size() > 1) {
            collection.insertMany(dataBSON);
        } else {
            _insertOnce(false);
        }
    }

    private Object _insertOnce(boolean rsState) {
        ObjectId oid;
        String rString = "";
        try {
            collection.insertOne(dataBSON.get(0));
            if (rsState) {
                oid = (ObjectId) (dataBSON.get(0).get("_id"));
                if (oid != null) {
                    rString = oid.toString();
                }
            }
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return rString;
    }

    public JSONObject getAndUpdate() {
        Bson updateData;
        Bson filterData = translate2bsonAndRun();
        updateData = document2updateBSON(false);
        try {
            if (filterData != null && updateData != null) {
                return bson2json(collection.findOneAndUpdate(filterData, updateData));
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return null;
    }

    public boolean update() {
        Bson updateData;
        Bson filterData = translate2bsonAndRun();
        updateData = document2updateBSON(false);
        try {
            if (filterData != null && updateData != null) {
                return collection.updateOne(filterData, updateData).getModifiedCount() > 0;
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return false;
    }

    public long updateAll() {
        Bson updateDatas;
        UpdateResult result = null;
        Bson filterData = translate2bsonAndRun();
        try {
            if (filterData == null) {
                filterData = new BasicDBObject();
            }
            if (dataBSON.size() > 0) {
                updateDatas = document2updateBSON(true);
                result = collection.updateMany(filterData, Objects.requireNonNull(updateDatas));
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
            result = null;
        } finally {
            reinit();
        }
        return result != null ? result.getModifiedCount() : 0;
    }

    public JSONObject getAndDelete() {
        try {
            Bson filterData = translate2bsonAndRun();
            if (filterData != null) {
                return bson2json(collection.findOneAndDelete(filterData));
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return null;
    }

    public boolean delete() {
        try {
            Bson filterData = translate2bsonAndRun();
            if (filterData != null) {
                return collection.deleteOne(filterData).getDeletedCount() > 0;
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return false;
    }

    public long deleteAll() {
        DeleteResult result = null;
        try {
            Bson filterData = translate2bsonAndRun();
            if (filterData == null) {
                filterData = new BasicDBObject();
            }
            result = collection.deleteMany(filterData);
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return result != null ? result.getDeletedCount() : 0;
    }

    public boolean inc(String fieldName) {
        return add(fieldName, 1);
    }

    public JSONObject getAndInc(String fieldName) {
        return getAndAdd(fieldName, 1);
    }

    public boolean dec(String fieldName) {
        return add(fieldName, -1);
    }

    public JSONObject getAndDec(String fieldName) {
        return getAndAdd(fieldName, -1);
    }

    public JSONObject getAndAdd(String fieldName, long num) {
        updateBSON.add(Updates.inc(fieldName, num));
        return getAndUpdate();
    }

    public boolean add(String fieldName, long num) {
        updateBSON.add(Updates.inc(fieldName, num));
        return update();
    }

    public JSONObject find() {
        try {
            return bson2json(_find().first());
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        }
        return null;
    }

    public JSONArray<JSONObject> select() {
        Document doc;
        JSONObject json;
        JSONArray<JSONObject> rs = new JSONArray<>();
        //解析内容，执行之
        try {
            FindIterable<Document> fd = _find();
            for (Document document : fd) {
                doc = document;
                json = bson2json(doc);
                if (json != null) {
                    rs.add(json);
                }
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
            rs = null;
        }
        return rs;
    }

    private FindIterable<Document> _find() {
        Bson bson;
        FindIterable<Document> fd = null;
        try {

            Bson filterData = translate2bsonAndRun();
            fd = filterData == null ? collection.find() : collection.find(filterData);

            if (fieldVisible.size() > 0 || fieldDisable.size() > 0)
                fd = fd.projection(reBuildField());
            if (sortBSON.size() > 0) {
                bson = Sorts.orderBy(sortBSON);
                fd = fd.sort(bson);
            }
            if (skipNo > 0)
                fd = fd.skip(skipNo);
            if (limitNo > 0)
                fd = fd.limit(limitNo);
        } catch (Exception e) {
            // errout();
            nLogger.logInfo(e);
        } finally {
            reinit();
        }
        return fd;
    }

    public Mongodb distinct() {
        _distinct = true;
        return this;
    }

    public JSONArray<JSONObject> group() {
        return group(null);
    }

    /**
     * @param groupName //groupby fieldName
     * @return
     */
    public JSONArray<JSONObject> group(String groupName) {
        JSONArray<JSONObject> rs = new JSONArray<>();
        List<Bson> ntemp = new ArrayList<>();
        List<BsonField> groupParamts = new ArrayList<>();
        Bson filterData = translate2bsonAndRun();
        String groupString = groupName == null ? null : "$" + groupName;
        String _valueName = groupbyfield == null || groupbyfield.equals("") ? groupName : groupbyfield;

        if (filterData != null)
            ntemp.add(Aggregates.match(filterData));
        if (fieldBSON.size() > 0)
            ntemp.add(Aggregates.project(reBuildField()));
        if (_count)
            groupParamts.add(Accumulators.sum("count", 1));
        if (_sum)
            groupParamts.add(Accumulators.sum("total", "$" + _valueName));
        if (_max)
            groupParamts.add(Accumulators.max("max", "$" + _valueName));
        if (_min)
            groupParamts.add(Accumulators.min("min", "$" + _valueName));
        if (_avg)
            groupParamts.add(Accumulators.avg("avg", "$" + _valueName));
        ntemp.add(Aggregates.group(groupString, groupParamts));
        if (sortBSON.size() > 0)
            ntemp.add(Aggregates.sort(Sorts.orderBy(sortBSON)));
        if (skipNo > 0)
            ntemp.add(Aggregates.skip(skipNo));
        if (limitNo > 0)
            ntemp.add(Aggregates.limit(limitNo));
        try {
            AggregateIterable<Document> fd = collection.aggregate(ntemp);
            for (Document item : fd) {
                rs.add(bson2json(item));
            }
        } catch (Exception e) {
            //errout();
            nLogger.logInfo(e);
            rs = null;
        } finally {
            reinit();
        }
        return rs;
    }

    /**
     * @param islist 是否是链式，链式不清除条件
     * @return
     */
    public long count(boolean islist) {
        long rl = 0;

        //System.out.println(condString());
        try {
            Bson filterData = translate2bsonAndRun();
            rl = filterData == null ? collection.estimatedDocumentCount() : collection.countDocuments(filterData);
            if (!islist) {
                reinit();
            }
        } catch (Exception e) {
            nLogger.logInfo(e, "Mongodb.count,返回集为空，对象表单未设置");
        }
        return rl;
    }

    public String getConditionString() {
        return conditionJSON.toString();
    }


    public JSONArray<String> distinct(String fieldName) {
        JSONArray<String> rTs = new JSONArray<>();
        Bson filterData = translate2bsonAndRun();
        DistinctIterable<String> fd;
        try {
            fd = filterData != null ? collection.distinct(fieldName, filterData, String.class) : collection.distinct(fieldName, String.class);
            for (String item : fd) {
                rTs.add(item);
            }
        } catch (Exception e) {
            nLogger.logInfo(e);
            rTs = null;
        } finally {
            reinit();
        }
        return rTs;
    }
    //透明分表mongodb不需要

    public JSONArray<JSONObject> page(int pageidx, int pagemax) {//普通分页
        return skip((pageidx - 1) * pagemax).limit(pagemax).select();
    }

    public JSONArray<JSONObject> page(int pageidx, int pagemax, Object lastId, String fastField) {//普通分页
        return skip((pageidx - 1) * pagemax).limit(pagemax).gt(fastField, lastId).select();
    }

    public long count() {
        return count(false);
    }

    public Mongodb count(String groupbyString) {//某字段分组后数量
        groupbyfield = groupbyString;
        _count = true;
        return this;
    }

    public Mongodb max(String groupbyString) {
        groupbyfield = groupbyString;
        _max = true;
        return this;
    }

    public Mongodb min(String groupbyString) {
        groupbyfield = groupbyString;
        _min = true;
        return this;
    }

    public Mongodb avg(String groupbyString) {
        groupbyfield = groupbyString;
        _avg = true;
        return this;
    }

    public Mongodb sum(String groupbyString) {
        groupbyfield = groupbyString;
        _sum = true;
        return this;
    }

    /**
     * 多线程同步扫描
     *
     * @param func
     * @param max
     * @param synNo
     * @return
     */

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

        long rl = dirty().count();
        int maxCount = (int) rl;
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        ConcurrentHashMap<Integer, JSONArray<JSONObject>> tempResult;
        tempResult = new ConcurrentHashMap<>();

        List<List<Object>> condJSON = getCond();
        String _formName = getFullForm();

        for (int index = 1; index <= pageNO; index++) {
            final int _index = index;
            final int _max = max;
            threadPool.execute(() -> {
                try {
                    Mongodb db = new Mongodb(_configString);
                    db.form(_formName);
                    db.setCond(condJSON);
                    var jsonArr = db.page(_index, _max);
                    tempResult.put(_index, Objects.requireNonNull(func).apply(jsonArr));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        JSONArray<JSONObject> rArray = new JSONArray<>();
        for (int key : tempResult.keySet()) {
            rArray.addAll(tempResult.get(key));
        }
        return rArray;
    }


    public JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max) {
        if (func == null) {
            nLogger.logInfo("scan 过滤函数不存在");
        }
        if (max <= 0) {
            nLogger.logInfo("scan 每页最大值不能小于等于0");
        }
        long rl = dirty().count();
        int maxCount = (int) rl;
        int pageNO = maxCount % max > 0 ? (maxCount / max) + 1 : maxCount / max;
        JSONArray<JSONObject> jsonArr, tempResult;
        tempResult = new JSONArray<>();
        for (int index = 1; index <= pageNO; index++) {
            jsonArr = dirty().page(index, max);
            tempResult.addAll(Objects.requireNonNull(func).apply(jsonArr));
        }
        return tempResult;
    }

    public List<List<Object>> getCond() {
        return conditionJSON;
    }

    public Mongodb setCond(List<List<Object>> conJSON) {
        conditionJSON = conJSON;
        return this;
    }

    public Mongodb groupWhere(JSONArray<JSONObject> condArray) {
        return groupCondition(DbFilter.buildDbFilter(condArray).buildEx());
    }

    public Mongodb groupCondition(List<List<Object>> conds) {
        if (conds != null && conds.size() > 0) {
            List<Object> block = new ArrayList<>();
            block.add(conditiobLogicAnd ? "and" : "or");
            block.add(conds);
            conditionJSON.add(block);
        }
        return this;
    }

    //document 2 updateBson
    private Bson document2updateBSON(boolean isMany) {
        List<Document> _dataBSON;
        if (dataBSON.size() + updateBSON.size() == 0) {
            return null;
        }
        if (isMany) {
            _dataBSON = dataBSON;
        } else {
            _dataBSON = new ArrayList<>();
            if (dataBSON.size() > 0) {
                _dataBSON.add(dataBSON.get(0));
            }
        }
        for (Document doc : _dataBSON) {
            doc.remove("_id");
            for (Object item : doc.keySet()) {
                updateBSON.add(Updates.set(item.toString(), doc.get(item)));
            }
        }
        return Updates.combine(updateBSON);
    }

    private BasicDBObject translate2bsonAndRun() {//翻译到BSON并执行
        BasicDBObject rBSON = new BasicDBObject();
        int size = conditionJSON.size();
        if (size > 0) {
            List<Object> tempConds = new ArrayList<>(conditionJSON);
            rBSON = translate2bsonAndRun(tempConds);
            // rBSON = killAnd(rBSON);  // 最外层 $and 转成 obj
        }
        return rBSON;
    }

    // 通KEY合并 r->l
    private BasicDBObject BasicDBObjectAppend(BasicDBObject l, BasicDBObject r) {
        for (Object rk : r.keySet()) {
            BasicDBList lvl;
            if (l.containsKey(rk)) {
                Object lv = l.get(rk);
                if (lv instanceof BasicDBObject) {
                    lvl = new BasicDBList();
                    lvl.add(lv);
                } else {
                    lvl = (BasicDBList) lv;
                }
                Object rv = r.get(rk);
                if (rv instanceof BasicDBObject) {
                    lvl.add(rv);
                } else {
                    BasicDBList rvl = (BasicDBList) rv;
                    lvl.addAll(rvl);
                }
                l.put(rk.toString(), lvl);
            } else {
                l.put(rk.toString(), r.get(rk));
            }
        }
        return l;
    }

    //返回BasicDBList或者BasicDBObject
    private BasicDBObject translate2bsonAndRun(List<Object> conds) {
        BasicDBObject r = new BasicDBObject();
        for (Object item : conds) {
            // r = new BasicDBObject();
            Object idx0 = conds.get(0);
            if (item instanceof ArrayList) {//列表对象是list
                List<Object> info = (List<Object>) item;
                BasicDBObject cond = translate2bsonAndRun(info);
                if (cond.size() > 0) {
                    BasicDBList tempInfoList = (BasicDBList) r.get("$" + info.get(0));
                    if (tempInfoList == null) {
                        tempInfoList = new BasicDBList();
                    }
                    tempInfoList.add(cond);
                    r.put("$" + info.get(0), tempInfoList);
                }
            } else {
                if (conds.size() == 2) {//是条件组
                    BasicDBObject rInfo = new BasicDBObject();
                    if (idx0 instanceof String) {
                        rInfo = translate2bsonAndRun((List<Object>) conds.get(1));
                    }
                    return rInfo;
                }
            }
            if (conds.size() == 4) {//是条件
                if (idx0 instanceof String) {
                    BasicDBObject cond = new BasicDBObject();
                    String logicStr = logic2mongodb((String) conds.get(2));
                    String field = (String) conds.get(1);
                    Object value = conds.get(3);
                    if (logicStr.equalsIgnoreCase("$regex")) {
                        value = "^.*" + value.toString() + ".*$";
                    } else {
                        value = _getID(field, value);
                    }
                    BasicDBObject rInfo = new BasicDBObject();
                    /*  // 简化 eq
                    if( logicStr.equals("$eq") ){
                        rInfo.put(field, value);
                    }
                    else{
                        cond.put(logicStr, value);
                        rInfo.put(field, cond);
                    }
                     */
                    cond.put(logicStr, value);
                    rInfo.put(field, cond);
                    return rInfo;
                }
            }
        }
        return r;
    }
    //bson 2 jsonObject

    //jsonObject string 2 bson object
    private Document json2document(JSONObject _json) {
        return new Document(_json);
    }

    public Mongodb bind(String ownerID) {
        if (!ownerID.equals(ownid)) {
            ownid = ownerID;
            form(formName);
        }
        return this;
    }

    public String getGeneratedKeys() {
        return "_id";
    }

    public String getFormName() {
        return formName;
    }

    public Mongodb dirty() {
        isDirty = true;
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

    //创建新表，仅仅和满足接口而已
    public Mongodb newTable() {
        return this;
    }

    public void clear() {
        isDirty = false;
        reinit();
    }

    //创建新临时表，仅仅和满足接口而已
    public Mongodb newTempTable() {
        return this;
    }

    public List<String> getAllTables() {
        List<String> rArray = new ArrayList<>();
        MongoIterable<String> clist = mongoDatabase.listCollectionNames();
        for (String s : clist) {
            rArray.add(s);
        }
        return rArray;
    }

    public boolean run(String cmd) {
        System.out.println("不支持:" + cmd);
        return false;
    }

    /**
     *
     */
    public static class mongodbConfig {
        private String database;

        public MongoClientURI json2mongouri(String jsonConfig) {
            MongoClientURI rs;
            String user;
            String password;
            StringBuilder nodeString = new StringBuilder();
            String authString;
            String repsetName;
            String secMethod;
            int maxPoolSize;

            JSONObject obj = JSONObject.toJSON(jsonConfig);
            user = obj.getString("user");
            password = obj.getString("password");
            database = obj.getString("database");
            repsetName = obj.getString("replicaSet");
            secMethod = obj.getString("sec");

            JSONArray<JSONObject> nodes = obj.getJsonArray("nodeAddresses");
            maxPoolSize = obj.getInt("maxTotal");
            if (maxPoolSize <= 0) {
                maxPoolSize = 150;
            }
            for (Object node : nodes) {
                nodeString.append(node).append(",");
            }
            authString = "";
            if (!user.equals("") && !password.equals("")) {
                authString = user + ":" + password + "@";
            }

            nodeString = new StringBuilder(nodeString.substring(0, nodeString.length() - 1));
            String url = "mongodb://" + authString + nodeString + "/?authSource=" + database;

            if (!secMethod.equals("")) {
                url += "&authMechanism=" + secMethod;
            }

            url += "&maxPoolSize=" + maxPoolSize + "&waitQueueMultiple=5000";
            if (repsetName != null && repsetName.length() > 2) {
                url += "&replicaSet=" + repsetName;
            }

            // MongoCredential mcr = MongoCredential.createCredential(user, database, password.toCharArray() );

            MongoClientOptions.Builder build = new MongoClientOptions.Builder();
            build.sslEnabled(obj.getBoolean("ssl"));

            rs = new MongoClientURI(url, build);

            return rs;
        }
    }

    public String func(String str) {
        return "";
    }

    public String now() {
        return TimeHelper.build().nowDatetime();
    }

    /**
     * 10位unixtime
     */
    public String formUnixTime(long unixTime) {
        return TimeHelper.build().timestampToDatetime(unixTime);
    }

    public String curDate() {
        return TimeHelper.build().nowDate();
    }

    public String curTime() {
        return TimeHelper.build().nowTime();
    }


}
