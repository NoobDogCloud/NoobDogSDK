package common.java.Apps.MicroService.Model;

import common.java.GscCommon.CheckModel;
import common.java.ServiceTemplate.SuperItemField;
import org.json.gsc.JSONObject;

import java.util.HashMap;

/**
 *
 */
public class MModelSuperField {
    private static final HashMap<String, Object> waitting = new HashMap<>();

    static {
        // 这里要补齐的默认DB字段
        waitting.put(SuperItemField.fatherField, "");
        waitting.put(SuperItemField.deleteField, 0);
        waitting.put(SuperItemField.visibleField, 0);
        waitting.put(SuperItemField.levelField, 0);
        waitting.put(SuperItemField.sortField, 0);
        waitting.put(SuperItemField.createAtField, ":timestamp");
        waitting.put(SuperItemField.updateAtField, ":timestamp");
        waitting.put(SuperItemField.removeAtField, "");
        waitting.put(SuperItemField.statusField, CheckModel.running);
    }

    /**
     * 补上没有定义的gsc-tree字段
     */
    protected HashMap<String, MModelRuleNode> ruleNodeFilter(HashMap<String, MModelRuleNode> map) {
        HashMap<String, Object> nowWaiting = (HashMap<String, Object>) waitting.clone();
        //生成需要补充的字段
        HashMap<String, Object> newWaiting = (HashMap<String, Object>) nowWaiting.clone();
        for (String fieldName : map.keySet()) {
            if (nowWaiting.containsKey(fieldName)) {
                newWaiting.remove(fieldName);
            }
        }
        // 剩下没有包含的字段
        for (String fieldName : newWaiting.keySet()) {
            map.put(fieldName, this.createEmptyNode(fieldName, newWaiting.get(fieldName)));
        }
        return map;
    }

    private MModelRuleNode createEmptyNode(String fieldName, Object initVal) {
        JSONObject node = new JSONObject();
        node.put("name", fieldName)
                .put("type", 0)
                .put("init", initVal)
                .put("failed", initVal)
                .put("checkId", 1);
        return new MModelRuleNode(node);
    }
}
