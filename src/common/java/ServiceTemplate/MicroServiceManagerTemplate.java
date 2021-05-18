package common.java.ServiceTemplate;

import common.java.InterfaceModel.Type.ApiType;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.function.Consumer;

/**
 * 该模板默认修改数据必须在会话下
 */
public class MicroServiceManagerTemplate extends MicroServiceTemplate {
    public MicroServiceManagerTemplate(String ModelName) {
        super(ModelName);
    }

    public MicroServiceManagerTemplate(String ModelName, Consumer<MicroServiceTemplate> fn) {
        super(ModelName, fn);
    }

    @ApiType(ApiType.type.SessionApi)
    public Object insert(JSONObject json) {
        return super.insert(json);
    }

    @ApiType(ApiType.type.SessionApi)
    public int delete(String uids) {
        return super.delete(uids);
    }

    @ApiType(ApiType.type.SessionApi)
    public int deleteEx(JSONArray cond) {
        return super.deleteEx(cond);
    }

    @ApiType(ApiType.type.SessionApi)
    public int update(String uids, JSONObject json) {
        return super.update(uids, json);
    }

    @ApiType(ApiType.type.SessionApi)
    public int updateEx(JSONObject json, JSONArray cond) {
        return super.updateEx(json, cond);
    }
}
