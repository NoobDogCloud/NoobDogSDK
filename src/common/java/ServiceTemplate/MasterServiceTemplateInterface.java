package common.java.ServiceTemplate;

import common.java.InterfaceModel.Type.InterfaceType;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

public interface MasterServiceTemplateInterface {
    /**
     * @param data
     *  新增数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String insert(JSONObject data);

    /**
     * @param ids 主键组（不同主键值用“,”隔开）
     *  删除数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String delete(String ids);

    /**
     * @param conditions gsc-GraphQL
     *  删除数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String deleteEx(JSONArray conditions);

    /**
     * @param ids        主键组（不同主键值用“,”隔开）
     * @param updateData 更新的数据
     *  更新数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String update(String ids, JSONObject updateData);

    /**
     * @param updateData 更新的数据
     * @param conditions gsc-GraphQL
     *  更新数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String updateEx(JSONObject updateData, JSONArray conditions);

    /**
     * @param idx 当前页码
     * @param max 每页最大显示数量
     *  分页方式展示数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String page(int idx, int max);

    /**
     * @param idx  当前页码
     * @param max  每页最大显示数量
     * @param conditions gsc-GraphQL
     *  页方式展示数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String pageEx(int idx, int max, JSONArray conditions);

    /**
     * 获得全部数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    // @InterfaceType(InterfaceType.type.OauthApi)
    String select();

    /**
     *  获得全部数据
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    String select(String appId);

    /**
     * @param conditions gsc-GraphQL
     *  获得全部数据, 通过指定条件
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    @InterfaceType(InterfaceType.type.OauthApi)
    String selectEx(JSONArray conditions);

    /***
     *  查找指定数据
     * @param key 查找的字段名
     * @param val 查找的字段值
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String find(String key, Object val);

    /***
     *  查找指定数据, 通过指定条件
     * @param conditions gsc-GraphQL
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String findEx(JSONArray conditions);

    /**
     *  根据条件获得以符合条件的数据为ROOT的构造JSON-TREE
     */
    @InterfaceType(InterfaceType.type.SessionApi)
    String tree(JSONArray conditions);
}
