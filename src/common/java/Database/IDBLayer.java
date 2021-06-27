package common.java.Database;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.List;
import java.util.function.Function;

public interface IDBLayer<T> {
    // T addFieldOutPipe(String fieldName, Function<Object, Object> func);

    // T addFieldInPipe(String fieldName, Function<Object, Object> func);

    // JSONArray selectByCache(int second);

    // void invalidCache();

    // JSONObject findByCache(int second);

    // JSONObject findByCache();

    // void Close();

    void addConstantCond(String fieldName, Object CondValue);

    T and();

    T or();

    boolean nullCondition();

    T where(JSONArray<JSONObject> condArray);

    T groupCondition(List<List<Object>> conds);

    T groupWhere(JSONArray<JSONObject> conds);

    T eq(String field, Object value);

    T ne(String field, Object value);

    T gt(String field, Object value);

    T lt(String field, Object value);

    T gte(String field, Object value);

    T lte(String field, Object value);

    T like(String field, Object value);

    T data(String jsonString);

    List<JSONObject> data();

    T data(JSONObject doc);

    List<JSONObject> clearData();

    T field();

    T field(String[] fieldString);

    T mask(String[] fieldString);

    T form(String _formName);

    T skip(int no);

    T limit(int no);

    T asc(String field);

    T desc(String field);

    List<Object> insert();

    boolean update();

    JSONObject getAndUpdate();

    long updateAll();

    boolean delete();

    JSONObject getAndDelete();

    long deleteAll();

    boolean inc(String fieldName);

    JSONObject getAndInc(String fieldName);

    boolean dec(String fieldName);

    JSONObject getAndDec(String fieldName);

    boolean add(String fieldName, long num);

    JSONObject getAndAdd(String fieldName, long num);

    // boolean sub(String fieldName, long num);

    // JSONObject getAndSub(String fieldName, long num);

    JSONObject find();

    JSONArray<JSONObject> select();

    JSONArray<JSONObject> group();

    JSONArray<JSONObject> group(String groupName);

    JSONArray<String> distinct(String fieldName);

    JSONArray<JSONObject> page(int pageIdx, int pageMax);

    JSONArray<JSONObject> page(int pageIdx, int pageMax, Object lastId, String fastField);

    long count();

    T count(String groupByString);

    T max(String groupByString);

    T min(String groupByString);

    T avg(String groupByString);

    T sum(String groupByString);

    String getFormName();

    String getForm();

    String getFullForm();

    void asyncInsert();

    Object insertOnce();

    T bind(String ownerID);

    // T bind();

    int limit();

    int pageMax(int max);

    String getGeneratedKeys();

    T dirty();

    void clear();

    JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max);

    JSONArray<JSONObject> scan(Function<JSONArray<JSONObject>, JSONArray<JSONObject>> func, int max, int synNo);

    List<List<Object>> getCond();

    T setCond(List<List<Object>> conJSON);

    List<String> getAllTables();

    String getConditionString();

    boolean run(String cmd);

    String func(String str);

    String now();

    String formUnixTime(long unixTime);

    String curDate();

    String curTime();
}
