package common.java.ServiceTemplate;

import common.java.Database.DBLayer;
import common.java.Http.Server.HttpContext;
import common.java.InterfaceModel.Type.InterfaceType;
import common.java.Rpc.RpcPageInfo;
import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public class MasterServiceTemplate implements MicroServiceTemplateInterface {
    private DBLayer fdb;

    public MasterServiceTemplate() {
    }

    public MasterServiceTemplate(String tableName) {
        init(tableName);
    }

    /**
     * 获得fastDB 设置各类操作回调
     */
    public DBLayer getPureDB() {
        fdb.clear();
        return fdb;
    }

    @InterfaceType(InterfaceType.type.CloseApi)
    public void init(String tableName) {
        fdb = DBLayer.build();
        fdb.form(tableName);
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    @Override
    public JSONArray select() {
        return fdb.select();
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    public JSONArray select(String appID) {
        return fdb.eq("appid", appID).select();
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    @Override
    public JSONArray selectEx(JSONArray cond) {
        if (fdb.where(JSONArray.toJSONArray(cond)).nullCondition()) {
            return null;
        }
        return select();
    }

    /**
     * 分页方式
     *
     * @param idx 当前页码
     * @param max 每页最大数量
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public RpcPageInfo page(int idx, int max) {
        return RpcPageInfo.Instant(idx, max, fdb.dirty().count(), fdb.page(idx, max));
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public RpcPageInfo pageEx(int idx, int max, JSONArray cond) {
        if (fdb.where(cond).nullCondition()) {
            return null;
        }
        return page(idx, max);
    }

    /**
     * 更新计划任务信息
     *
     * @param uidArr 用,分开的id组
     * @param json   GSC-FastJSON:更新的内容
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public int update(String uidArr, JSONObject json) {
        return _update(uidArr, json, null);
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public int updateEx(JSONObject json, JSONArray cond) {
        return _update(null, json, cond);
    }

    private int _update(String uidArr, JSONObject info, JSONArray cond) {
        if (JSONObject.isInvalided(info)) {
            return 0;
        }
        if (!StringHelper.isInvalided(HttpContext.current().appId())) {//非管理员情况下
            info.remove("appid");
        }
        if (fdb.where(cond).nullCondition()) {
            return 0;
        }
        fdb.data(info);
        return (int) (uidArr != null ? fdb.putAllOr(uidArr).updateAll() : fdb.updateAll());
    }


    /**
     * 删除计划任务信息
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public int delete(String uidArr) {
        return (int) (fdb.putAllOr(uidArr).nullCondition() ? 0 : fdb.deleteAll());
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public int deleteEx(JSONArray cond) {
        return (int) (fdb.where(cond).nullCondition() ? 0 : fdb.deleteAll());
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public String insert(JSONObject nObj) {
        String rString = null;
        if (nObj != null) {
            nObj.put("appid", HttpContext.current().appId());
            rString = StringHelper.toString(fdb.data(nObj).insertOnce());
        }
        return rString;
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public JSONObject find(String key, String val) {
        return fdb.eq(key, val).find();
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    public JSONArray<JSONObject> findArray(String key, String val) {
        return fdb.eq(key, val).select();
    }

    @InterfaceType(InterfaceType.type.SessionApi)
    @Override
    public JSONObject findEx(JSONArray cond) {
        return fdb.where(cond).nullCondition() ? null : fdb.find();
    }

    @Override
    public String tree(JSONArray cond) {
        return "";
    }
}
