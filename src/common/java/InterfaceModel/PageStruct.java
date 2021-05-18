package common.java.InterfaceModel;


import org.json.gsc.JSONArray;

public class PageStruct {
    private final long idx;
    private final long max;
    private final long count;
    private final JSONArray data;

    public PageStruct(long idx, long max, long count, JSONArray data) {
        this.idx = idx;
        this.max = max;
        this.count = count;
        this.data = data;
    }

    public long idx() {
        return idx;
    }

    public long max() {
        return max;
    }

    public long count() {
        return count;
    }

    public JSONArray data() {
        return data;
    }
}
