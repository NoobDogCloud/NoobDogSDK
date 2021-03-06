package common.java.Apps.MicroService.Model;

import common.java.Apps.MicroService.Model.Interface.MModelApiPerm;
import common.java.Apps.MicroService.Model.RBAC.MModelPerm;
import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MicroModel {
    private final String appId;
    private String tableName;
    private boolean softMode;
    private String pkField;
    private MModelRuleArray mmrArray;
    private MModelPerm mmps;

    private MModelApiPerm apips;

    // public MicroModel(String appId, String tableName, String pkField, JSONObject modelJson) {
    public MicroModel(String appId, JSONObject modelJson) {
        this.appId = appId;
        if (modelJson != null) {
            this.tableName = modelJson.getString("tableName");
            this.pkField = modelJson.getString("primaryKey");
            this.softMode = modelJson.getBoolean("softMode");
            this.mmps = new MModelPerm(appId, modelJson.getJson("perm"));
            this.mmrArray = new MModelRuleArray(modelJson.getJsonArray("rule"));
            this.apips = new MModelApiPerm(modelJson.getJson("api"));
        }
    }

    /**
     * 获得模型表名称
     */
    public String tableName() {
        return this.tableName;
    }

    /**
     * 获得模型主键字段名称
     */
    public String pkField() {
        return this.pkField;
    }

    /**
     * 获得模型删除模式
     */
    public boolean softMode() {
        return this.softMode;
    }

    /**
     * 获得规则组hashmap
     */
    public HashMap<String, MModelRuleNode> rules() {
        return this.mmrArray.self();
    }

    /**
     * 获得规则组
     */
    public MModelRuleArray ruleArray() {
        return this.mmrArray;
    }

    /**
     * 获得权限组
     */
    public MModelPerm perms() {
        return this.mmps;
    }

    /**
     * 获得API权限组
     */
    public MModelApiPerm apiPerms() {
        return this.apips;
    }

    /**
     * 数据JSON结构的微服务模型
     */
    public JSONObject toJson() {
        return JSONObject.build("rule", this.mmrArray.toJsonArray())
                .put("permissions", this.mmps.toJson());
    }

    public String getAppId() {
        return this.appId;
    }
}
