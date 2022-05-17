package common.java.Rpc;

import common.java.Http.Server.HttpContext;
import org.json.gsc.JSONObject;

public class Filter {
    private final JSONObject data;

    private Filter(JSONObject data) {
        this.data = data;
    }

    public static Filter current() {
        var ctx = HttpContext.current();
        return ctx == null ? null : new Filter(ctx.getFilterExtends());
    }


    public Filter add(String name, Object value) {
        data.put(name, value);
        return this;
    }

    public Filter remove(String name) {
        data.remove(name);
        return this;
    }

}
