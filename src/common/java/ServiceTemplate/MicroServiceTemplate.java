package common.java.ServiceTemplate;

import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.MicroService.Model.MModelRuleNode;
import common.java.Check.CheckResult;
import common.java.Database.DBFilter;
import common.java.Database.DBLayer;
import common.java.Http.Server.Db.HttpContextDb;
import common.java.Http.Server.HttpContext;
import common.java.InterfaceModel.GrapeTreeDbLayerModel;
import common.java.InterfaceModel.Type.Aggregation;
import common.java.OAuth.oauthApi;
import common.java.Rpc.RpcPageInfo;
import common.java.Rpc.rMsg;
import common.java.Rpc.rpc;
import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class MicroServiceTemplate implements MicroServiceTemplateInterface {
    private final String[] aggr_key = {"", ""};
    public GrapeTreeDbLayerModel db;
    private String modelName;
    private Consumer<MicroServiceTemplate> InitDB_fn;
    // private boolean join_flag = true;

    public MicroServiceTemplate(String ModelName) {
        init(ModelName, null);
    }

    public MicroServiceTemplate(String ModelName, Consumer<MicroServiceTemplate> fn) {
        init(ModelName, fn);
    }

    private void init(String ModelName, Consumer<MicroServiceTemplate> fn) {
        this.modelName = ModelName;
        db = GrapeTreeDbLayerModel.getInstance(ModelName);
        if (fn != null) {
            InitDB_fn = fn;
            InitDB_fn.accept(this);
        }
        // 新增读取数据时前置操作
        db.readPipe(this::InitDBFilter);
        // 根据模型字段定义，生成 join
        var ruleArr = db.getMicroModel().rules();
        for (var node : ruleArr.values()) {
            var block = node.join();
            if (block != null) {
                outPipe(array ->
                        joinOn(node.name(), array, block.key(),
                                ids -> rpc.service(block.service()).setPath(block.item(), "find").call(block.key(), ids).asJsonArray()
                        )
                );
            }
        }
    }

    // 检查有约束的字段
    private CheckResult constraintFilter(JSONObject input) {
        if (input == null || JSONObject.isInvalided(input)) {
            return CheckResult.build(false, "all");
        }
        var ruleArr = db.getMicroModel().rules();
        for (var node : ruleArr.values()) {
            var block = node.constraint();
            if (block != null) {
                var key = node.name();
                // 获得 字段 对应值
                if (input.containsKey(key)) {
                    var v = input.get(node.name());
                    if (v != null) {
                        var response = rpc.service(block.service()).setPath(block.item(), "find").call(block.key(), v);
                        if (!response.status()) {   // 请求异常
                            return CheckResult.build(false, key);
                        }
                        if (JSONObject.isInvalided(response.asJson())) {    // 返回结果异常
                            return CheckResult.build(false, key);
                        }
                    }
                }
            }
        }
        return CheckResult.buildTrue();
    }

    /**
     * 响应新的字段限制，排序限制，功能开关
     */
    private void InitDBFilter(GrapeTreeDbLayerModel m) {
        HttpContextDb ctx = HttpContext.current().dbHeaderContext();
        if (ctx.hasFields()) {
            if (ctx.notIn()) {
                m.mask(ctx.fields());
            } else {
                m.field(ctx.fields());
            }
        }
        JSONObject sorts = ctx.sort();
        if (!JSONObject.isInvalided(sorts)) {
            for (String field : sorts.keySet()) {
                if (sorts.getString(field).equalsIgnoreCase("desc")) {
                    m.desc(field);
                } else {
                    m.asc(field);
                }
            }
        }
        JSONObject options = ctx.option();
        if (!JSONObject.isInvalided(options)) {
            // join开关
            if (options.containsKey("join")) {
                m.outPiperEnable(options.getBoolean("join"));
            }
        }
    }


    /**
     * 返回纯db对象
     */
    public DBLayer getPureDB() {
        return this.db.getPureDB();
    }

    /**
     * 返回经过过滤的db对象
     */
    protected GrapeTreeDbLayerModel getDB() {
        if (InitDB_fn != null) {
            InitDB_fn.accept(this);
        }
        return db;
    }

    public GrapeTreeDbLayerModel reset() {
        db.clear();
        return db;
    }

    public MicroServiceTemplate addInputFilter(String fieldName, Function<Object, Object> func) {
        this.db.addFieldInPipe(fieldName, func);
        return this;
    }

    public MicroServiceTemplate addOutputFilter(String fieldName, Function<Object, Object> func) {
        this.db.addFieldOutPipe(fieldName, func);
        return this;
    }

    protected JSONArray toJsonArray(Object o) {
        if (o instanceof JSONObject) {
            return JSONArray.build(o);
        }
        return (JSONArray) o;
    }

    protected JSONArray joinOn(String localKey, JSONArray localArray, String foreignKey, Function<String, JSONArray> func) {
        return joinOn(localKey, localArray, foreignKey, func, false);
    }

    protected JSONArray joinOn(String localKey, JSONArray localArray, String foreignKey, Function<String, JSONArray> func, boolean save_null_item) {
        if (JSONArray.isInvalided(localArray)) {
            return localArray;
        }
        List<String> ids = new ArrayList<>();
        // 构造模板ID条件组
        for (Object obj : localArray) {
            JSONObject _obj = (JSONObject) obj;
            if (JSONObject.isInvalided(_obj)) {
                continue;
            }
            String _ids = _obj.getString(localKey);
            if (StringHelper.isInvalided(_ids)) {
                continue;
            }
            // 填入多个值
            ids.addAll(Arrays.asList(_ids.split(",")));
        }
        int l = ids.size();
        if (l == 0) {
            return localArray;
        }
        String[] idsArray = ids.toArray(new String[l]);
        // 这里需要注意ids过多的情况
        int c = (l / 50) + (l % 50 > 0 ? 1 : 0);   // 总循环次数
        int p = 0;
        for (int i = 0; i < c; i++) {
            String _ids = StringHelper.join(idsArray, ",", p, 50);
            JSONArray foreignArray = func.apply(_ids);
            localArray.joinOn(localKey, foreignKey, foreignArray, save_null_item);
            p += 50;
        }
        // 设置返回数据
        return localArray;
    }

    protected JSONObject joinOn(String localKey, JSONObject localObject, String foreignKey, Function<String, JSONArray> func) {
        JSONArray newArray = joinOn(localKey, JSONArray.build(localObject), foreignKey, func);
        return JSONArray.isInvalided(newArray) ? localObject : (JSONObject) newArray.get(0);
    }

    public Object insert(JSONObject newData) {
        if (JSONObject.isInvalided(newData)) {
            return null;
        }
        // 输入数据对应字段值约束
        if (!constraintFilter(newData).isStatus()) {
            return null;
        }
        return db.data(newData).insertOnce();
    }

    @Override
    public int delete(String uids) {
        return _delete(uids, null);
    }

    @Override
    public int deleteEx(JSONArray cond) {
        return _delete(null, JSONArray.toJSONArray(cond));
    }

    private int _delete(String ids, JSONArray cond) {
        int r = 0;
        _ids(db.getGeneratedKeys(), ids);
        _condition(cond);
        if (!db.nullCondition()) {
            r = (int) db.deleteAll();
        }
        return r;
    }

    public int update(String uids, JSONObject data) {
        return _update(uids, data, null);
    }

    @Override
    public int updateEx(JSONObject info, JSONArray cond) {
        return _update(null, info, cond);
    }

    private int _update(String ids, JSONObject info, JSONArray cond) {
        if (JSONObject.isInvalided(info)) {
            return -1;
        }
        _ids(db.getGeneratedKeys(), ids);
        _condition(cond);
        if (db.nullCondition()) {
            return -1;
        }
        // 输入数据对应字段值约束
        if (!constraintFilter(info).isStatus()) {
            return -1;
        }
        return (int) db.data(info).updateAll();
    }

    @Override
    public RpcPageInfo page(int idx, int max) {
        return pageEx(idx, max, null);
    }

    public RpcPageInfo pageEx(int idx, int max, JSONArray cond) {
        _condition(cond);
        return RpcPageInfo.Instant(idx, max, db.dirty().count(), db.page(idx, max));
    }

    @Override
    public JSONArray select() {
        return selectEx(null);
    }

    public JSONArray selectEx(JSONArray cond) {
        _condition(cond);
        return db.select();
    }

    @Override
    public Object find(String field, String val) {
        int idNo = _ids(field, val);
        return idNo == 1 ? db.find() : db.select();
    }

    public JSONObject findEx(JSONArray cond) {
        _condition(cond);
        return db.find();
    }

    /**
     * @apiNote 获得tree-json结构的全表数据,获得行政机构json树
     */
    public Object tree(JSONArray cond) {
        Object rs;
        GrapeTreeDbLayerModel db = getDB();
        _condition(cond);
        long n = db.dirty().count();
        if (n != 1) {
            rs = false;
        } else {
            rs = db.getAllChildren();
        }
        return rs;
    }

    /**
     * @apiNote 微服务标准模板类独有API, 提供当前模型规则描述JSON给前端
     */
    public String getSafeDataModel() {
        HashMap<String, MModelRuleNode> mms = MicroServiceContext.current().model(this.modelName).rules();
        JSONObject desc = new JSONObject();
        for (String key : mms.keySet()) {
            MModelRuleNode mmrn = mms.get(key);
            if (mmrn.type() != MModelRuleNode.FieldType.maskField) {
                desc.put(key, mmrn.node());
            }
        }
        return rMsg.netMSG(true, desc);
    }

    public int _ids(String fieldName, String ids) {
        DBFilter dbf = DBFilter.buildDbFilter();
        if (StringHelper.isInvalided(ids)) {
            return 0;
        }
        String[] _ids = ids.split(",");
        if (_ids.length > 1) {
            Set<String> idsSet = new HashSet<>();
            idsSet.addAll(Arrays.asList(_ids));
            for (String id : idsSet) {
                dbf.or().eq(fieldName, id);
            }
            if (dbf.nullCondition()) {
                db.and().groupCondition(dbf.buildEx());
            }
        } else if (_ids.length == 1) {
            db.and().eq(fieldName, _ids[0]);
        }
        return _ids.length;
    }

    private void _condition(JSONArray cond) {
        if (!JSONArray.isInvalided(cond)) {
            db.and().where(cond);
        }
    }

    public boolean getApiAccessOnce(String className, String action) {
        HttpContext ctx = HttpContext.current();
        oauthApi.getInstance().getApiTokenService(ctx.serviceName(), className, action);
        return true;
    }

    public MicroServiceTemplate outPipe(Function<JSONArray, JSONArray> func) {
        db.outPipe(func);
        return this;
    }

    public void aggregation(Aggregation func) {
        db.outAggregation(func);
    }

    public MicroServiceTemplate setAggregationKey(String aggr_key) {
        this.aggr_key[0] = aggr_key;
        this.aggr_key[1] = aggr_key;
        return this;
    }

    public MicroServiceTemplate setAggregationKey(String local_key, String foreign_key) {
        this.aggr_key[0] = local_key;
        this.aggr_key[1] = foreign_key;
        return this;
    }

    public JSONArray aggregation(JSONArray store, JSONArray result) {
        JSONObject map = store.mapsByKey(aggr_key[0]);
        for (Object _o : result) {
            JSONObject o = (JSONObject) _o;
            String k = o.getString(aggr_key[1]);
            // 如果传递对象对应KEY有值，按字段覆盖替换
            if (map.containsKey(k)) {
                map.getJson(k).putAll(o);
            }
            // 如果不存在，直接填充
            else {
                map.put(aggr_key[0], o);
            }
        }
        return new JSONArray(map.values());
    }
}
