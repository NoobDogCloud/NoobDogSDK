package common.java.Apps.MicroService.Model;

import common.java.Apps.MicroService.Model.RBAC.MModelPerm;
import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MicroModel {
    private final int appId;
    private String tableName;
    private MModelRuleArray mmrArray;
    private MModelPerm mmps;

    public MicroModel(int appId, String tableName, JSONObject modelJson) {
        this.appId = appId;
        if (modelJson != null) {
            this.tableName = tableName;
            this.mmps = new MModelPerm(appId, modelJson.getJson("perm"));
            this.mmrArray = new MModelRuleArray(modelJson.getJsonArray("rule"));
        }
    }

    /**
     * 获得模型表名称
     */
    public String tableName() {
        return this.tableName;
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
     * 数据JSON结构的微服务模型
     */
    public JSONObject toJson() {
        return JSONObject.build("rule", this.mmrArray.toJsonArray())
                .put("permissions", this.mmps.toJson());
    }

}
