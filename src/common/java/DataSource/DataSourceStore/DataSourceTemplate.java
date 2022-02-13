package common.java.DataSource.DataSourceStore;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.List;

public class DataSourceTemplate {
    private JSONArray lines;
    private int status;             // 0 正常 1 等待删除

    private DataSourceTemplate() {
        lines = JSONArray.build();
        status = 0;
    }

    public static DataSourceTemplate build() {
        return new DataSourceTemplate();
    }

    public static DataSourceTemplate build(JSONObject v) {
        DataSourceTemplate tlp = new DataSourceTemplate();
        tlp.status = v.getInt("status");
        tlp.lines = v.getJsonArray("lines");
        return tlp;
    }

    public int getStatus() {
        return status;
    }

    public void setWaitDelete() {
        status = 1;
    }

    public DataSourceTemplate add(Object line) {
        lines.add(line);
        return this;
    }

    public JSONObject toJSON() {
        JSONObject json = JSONObject.build();
        json.put("lines", lines);
        json.put("status", status);
        return json;
    }

    public int size() {
        return lines.size();
    }

    public Object get(int idx) {
        return lines.get(idx);
    }

    public void clear() {
        lines.clear();
    }

    public List<Object> toArrayList() {
        return lines.toArrayList();
    }
}
