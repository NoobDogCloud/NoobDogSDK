package common.java.HttpServer.SpecHeader.Db;

import common.java.String.StringHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class HttpContextDb {
    public final static String fields = "GrapeDbFields";
    public final static String sorts = "GrapeDbSorts";
    public final static String options = "GrapeDbOptions";
    public static Set<String> DBHeaderKeys = new HashSet<>();

    static {
        DBHeaderKeys.add(fields);
        DBHeaderKeys.add(sorts);
        DBHeaderKeys.add(options);
    }

    private final JSONObject sort_object;
    private final JSONObject options_object;
    private final Set<String> field_array;
    private boolean field_logic_not = false;

    public HttpContextDb(JSONObject nHeader) {
        field_array = new HashSet<>();
        // 初始化字段组
        JSONArray t_field_array = nHeader.getJsonArray(fields);
        if (JSONArray.isInvalided(t_field_array)) {
            t_field_array = new JSONArray();
            JSONObject not_field_json = nHeader.getJson(fields);
            if (!JSONObject.isInvalided(not_field_json)) {
                for (String key : not_field_json.keySet()) {
                    if (key.equalsIgnoreCase("not")) {
                        field_logic_not = true;
                    }
                    t_field_array = not_field_json.getJsonArray(key);
                }
            }
        }
        field_array.addAll(t_field_array);
        // 初始化排序组
        sort_object = nHeader.getJson(sorts);
        // 初始化开关
        options_object = nHeader.getJson(options);
    }

    /**
     * 获得header里的DB参数
     */
    public JSONObject header(JSONObject nHeader) {
        JSONObject r = JSONObject.build(fields, nHeader.getString(fields))
                .put(sorts, nHeader.getString(sorts))
                .put(options, nHeader.getString(options));
        nHeader.remove(fields);
        nHeader.remove(sorts);
        nHeader.remove(options);
        return r;
    }

    public boolean notIn() {
        return field_logic_not;
    }

    public boolean hasFields() {
        return field_array.size() > 0;
    }

    public String fields() {
        StringBuilder r = new StringBuilder();
        for (String _o : field_array) {
            r.append(_o).append(",");
        }
        return StringHelper.build(r.toString()).trimFrom(',').toString();
    }

    public JSONObject sort() {
        return sort_object;
    }

    public JSONObject option() {
        return options_object;
    }
}
