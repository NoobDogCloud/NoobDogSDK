package common.java.Apps.MicroService.Model.RBAC;

import common.java.Authority.PermItemDef;
import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MModelPerm {
    private final String appId;
    private final JSONObject initPerm;
    private final HashMap<String, MModelPermInfo> permInfo;

    public MModelPerm(String appId, JSONObject pInfo) {
        if (pInfo == null) {
            pInfo = new JSONObject();
        }
        this.appId = appId;
        this.initPerm = pInfo;
        this.permInfo = new HashMap<>();
        buildPermInfoArr(PermItemDef.createMode);
        buildPermInfoArr(PermItemDef.updateMode);
        buildPermInfoArr(PermItemDef.readMode);
        buildPermInfoArr(PermItemDef.deleteMode);
        buildPermInfoArr(PermItemDef.adminMode);
    }

    private void buildPermInfoArr(String key) {
        if (initPerm.containsKey(key)) {
            JSONObject info = initPerm.getJson(key);
            if (!JSONObject.isInvalided(info)) {
                permInfo.put(key, MModelPermInfo.build(appId, info));
            }
        }
    }

    /**
     * 获得就的权限模型
     */
    public JSONObject getInitPerm() {
        return this.initPerm;
    }

    /**
     * 获得创建数据权限信息
     */
    public MModelPermInfo createPerm() {
        return this.permInfo.get(PermItemDef.createMode);
    }

    /**
     * 获得更新数据权限信息
     */
    public MModelPermInfo updatePerm() {
        return this.permInfo.get(PermItemDef.updateMode);
    }

    /**
     * 获得读取数据权限信息
     */
    public MModelPermInfo readPerm() {
        return this.permInfo.get(PermItemDef.readMode);
    }

    /**
     * 获得删除数据权限信息
     */
    public MModelPermInfo deletePerm() {
        return this.permInfo.get(PermItemDef.deleteMode);
    }

    /**
     * 获得管理员权限信息
     */
    public MModelPermInfo adminPerm() {
        return this.permInfo.get(PermItemDef.adminMode);
    }

    /**
     * 根据当前权限信息,输出权限结构数据
     */
    public JSONObject toJson() {
        JSONObject newPermJson = new JSONObject();
        for (String key : this.permInfo.keySet()) {
            newPermJson.put(key, this.permInfo.get(key).toJson());
        }
        return newPermJson;
    }

    /**
     * 根据权限模型定义,生成检查数据库入库的数据模型
     */

}
