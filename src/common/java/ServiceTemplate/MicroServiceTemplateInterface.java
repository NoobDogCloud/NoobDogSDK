package common.java.ServiceTemplate;

import common.java.InterfaceModel.Type.ApiType;
import common.java.Rpc.RpcPageInfo;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public interface MicroServiceTemplateInterface {
    /**
     * @param info gsc-json
     * @apiNote 新增数据
     */
    @ApiType(ApiType.type.SessionApi)
    Object insert(JSONObject info);

    /**
     * @param uids 主键组（不同主键值用“,”隔开）
     * @apiNote 删除数据
     */
    @ApiType(ApiType.type.SessionApi)
    int delete(String uids);

    /**
     * @param cond gsc-GraphQL
     * @apiNote 删除数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    int deleteEx(JSONArray cond);

    /**
     * @param uids 主键组（不同主键值用“,”隔开）
     * @param info gsc-json
     * @apiNote 更新数据
     */
    @ApiType(ApiType.type.SessionApi)
    int update(String uids, JSONObject info);

    /**
     * @param info gsc-json
     * @param cond gsc-GraphQL
     * @apiNote 更新数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    int updateEx(JSONObject info, JSONArray cond);

    /**
     * @param idx 当前页码
     * @param max 每页最大显示数量
     * @apiNote 分页方式展示数据
     */
    @ApiType(ApiType.type.SessionApi)
    RpcPageInfo page(int idx, int max);

    /**
     * @param idx  当前页码
     * @param max  每页最大显示数量
     * @param cond gsc-GraphQL
     * @apiNote 页方式展示数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    RpcPageInfo pageEx(int idx, int max, JSONArray cond);

    /**
     * @apiNote 获得全部数据
     */
    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    JSONArray select();

    /**
     * @param cond gsc-GraphQL
     * @apiNote 获得全部数据, 通过指定条件
     */
    @ApiType(ApiType.type.SessionApi)
    @ApiType(ApiType.type.OauthApi)
    JSONArray selectEx(JSONArray cond);

    /***
     * @apiNote 查找指定数据
     * @param key 查找的字段名
     * @param val 查找的字段值
     */
    @ApiType(ApiType.type.SessionApi)
    Object find(String key, String val);

    /***
     * @apiNote 查找指定数据, 通过指定条件
     * @param cond gsc-GraphQL
     */
    @ApiType(ApiType.type.SessionApi)
    JSONObject findEx(JSONArray cond);

    /**
     * @apiNote 根据条件获得以符合条件的数据为ROOT的构造JSON-TREE
     */
    @ApiType(ApiType.type.SessionApi)
    Object tree(JSONArray cond);

    /**
     * @apiNote 为特定的方法申请一次性授权
     * */
    // Object getApiAccessOnce(String className, String action);
}
