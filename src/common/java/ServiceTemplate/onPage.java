package common.java.ServiceTemplate;

import org.json.gsc.JSONArray;

public class onPage {
    private int idx;
    private int max;
    private JSONArray conds;

    public onPage(int idx, int max, JSONArray conds) {
        this.idx = idx;
        this.max = max;
        this.conds = conds;
    }

    public int idx() {
        return idx;
    }

    public void idx(int idx) {
        this.idx = idx;
    }

    public int max() {
        return max;
    }

    public void max(int max) {
        this.max = max;
    }

    public JSONArray conds() {
        return conds;
    }

    public void info(JSONArray conds) {
        this.conds = conds;
    }
}
