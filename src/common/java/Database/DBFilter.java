package common.java.Database;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DBFilter {
    private JSONArray condArray;
    private List<List<Object>> conds;
    private boolean conditiobLogicAnd;

    private DBFilter() {
        reinit();
    }

    private DBFilter(JSONArray cond) {
        reinit();
        if (cond != null) {
            condArray = cond;
        }
    }

    public static DBFilter buildDbFilter() {
        return new DBFilter();
    }

    public static DBFilter buildDbFilter(JSONArray cond) {
        return new DBFilter(cond);
    }

    private void reinit() {
        conditiobLogicAnd = true;
        condArray = new JSONArray();
        conds = new ArrayList<>();
    }

    public DBFilter and() {
        conditiobLogicAnd = true;
        return this;
    }

    public DBFilter or() {
        conditiobLogicAnd = false;
        return this;
    }

    public DBFilter eq(String field, Object value) {//One Condition

        addCondition(field, value, "=");
        return this;
    }

    public DBFilter ne(String field, Object value) {//One Condition

        addCondition(field, value, "!=");
        return this;
    }

    public DBFilter gt(String field, Object value) {//One Condition

        addCondition(field, value, ">");
        return this;
    }

    public DBFilter lt(String field, Object value) {//One Condition

        addCondition(field, value, "<");
        return this;
    }

    public DBFilter gte(String field, Object value) {//One Condition

        addCondition(field, value, ">=");
        return this;
    }

    public DBFilter lte(String field, Object value) {//One Condition

        addCondition(field, value, "<=");
        return this;
    }

    public DBFilter like(String field, Object value) {
        addCondition(field, value, "like");
        return this;
    }

    private void addCondition(String field, Object value, String logic) {
        JSONObject j = JSONObject.build("field", field).put("logic", logic).put("value", value);
        if (!conditiobLogicAnd) { // 是or
            j.put("link_logic", "or");
        }
        condArray.add(j);
    }

    public JSONArray build() {
        return condArray;
    }

    public DBFilter groupCondition(List<List<Object>> conds) {
        //List<List<Object>> nowConds = this.conds;
        List<Object> block = new ArrayList<>();
        block.add(conditiobLogicAnd ? "and" : "or");
        block.add(conds);
        this.conds.add(block);
        return this;
    }

    public boolean nullCondition() {
        return condArray.isEmpty();
    }

    public List<List<Object>> buildEx() {
        List<Object> bit;
        if (condArray.size() > 0) {
            for (Object obj : condArray) {
                bit = new ArrayList<>();
                JSONObject json = (JSONObject) obj;
                if (json.containsKey("link_logic")) {
                    bit.add(json.getString("link_logic").equalsIgnoreCase("and") ? "and" : "or");
                } else {
                    bit.add(conditiobLogicAnd ? "and" : "or");
                }
                bit.add(json.getString("field"));
                bit.add(json.getString("logic"));
                bit.add(json.get("value"));
                conds.add(bit);
            }
        }
        List<List<Object>> r = conds;
        reinit();
        return r;
    }
}
