package common.java.ServiceTemplate;

import common.java.InterfaceModel.Type.InterfaceType;
import common.java.Rpc.RpcPageInfo;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public interface MicroServiceTemplateInterface {
    /**
     * @param info gsc-json
     * 新增数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    Object insert(JSONObject info);

    /**
     * @param ids 主键组（不同主键值用“,”隔开）
     * 删除数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    int delete(String ids);

    /**
     * @param conditions gsc-GraphQL
     * 删除数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    int deleteEx(JSONArray conditions);

    /**
     * @param ids  主键组（不同主键值用“,”隔开）
     * @param info gsc-json
     * 更新数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    int update(String ids, JSONObject info);

    /**
     * @param info       gsc-json
     * @param conditions gsc-GraphQL
     * 更新数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    int updateEx(JSONObject info, JSONArray conditions);

    /**
     * @param idx 当前页码
     * @param max 每页最大显示数量
     * 分页方式展示数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    RpcPageInfo page(int idx, int max);

    /**
     * @param idx        当前页码
     * @param max        每页最大显示数量
     * @param conditions gsc-GraphQL
     * 页方式展示数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    RpcPageInfo pageEx(int idx, int max, JSONArray conditions);

    /**
     * 获得全部数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    JSONArray select();

    /**
     * @param cond gsc-GraphQL
     * 获得全部数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    JSONArray selectEx(JSONArray cond);

    /***
     * 查找指定数据
     * @param key 查找的字段名
     * @param val 查找的字段值
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    Object find(String key, String val);

    /***
     * 查找指定数据, 通过指定条件
     * @param conditions gsc-GraphQL
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    JSONObject findEx(JSONArray conditions);

    /**
     * 根据条件获得以符合条件的数据为ROOT的构造JSON-TREE
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    Object tree(JSONArray conditions);

    /**
     * 为特定的方法申请一次性授权
     * */
    // Object getApiAccessOnce(String className, String action);
}
