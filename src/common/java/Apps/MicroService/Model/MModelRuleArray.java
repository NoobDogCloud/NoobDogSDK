package common.java.Apps.MicroService.Model;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;

/**
 * 规则组
 */
public class MModelRuleArray extends MModelSuperField {
    private HashMap<String, MModelRuleNode> hashmap;

    public MModelRuleArray(JSONArray<JSONObject> rules) {
        init(rules);
    }

    private void init(JSONArray<JSONObject> rules) {
        hashmap = new HashMap<>();
        for (JSONObject l : rules) {
            MModelRuleNode mrn = new MModelRuleNode(l);
            hashmap.put(mrn.name(), mrn);
        }
        // 自动补齐模型未定义的gsc-tree模型必须字段
        hashmap = super.ruleNodeFilter(hashmap);
    }

    public HashMap<String, MModelRuleNode> self() {
        return hashmap;
    }

    public JSONArray toJsonArray() {
        JSONArray newRuleArray = new JSONArray();
        for (String key : hashmap.keySet()) {
            newRuleArray.put(hashmap.get(key).toJson());
        }
        return newRuleArray;
    }
}
