package common.java.Rpc;

import org.json.gsc.JSONObject;

import java.util.HashMap;

public class RpcBefore {
    // 过滤链(全局)
    public static final HashMap<String, FilterLink> filterArray = new HashMap<>();

    public static FilterReturn filter(String className, String actionName, Object[] input) {
        FilterLink fl = filterArray.get(className);
        FilterReturn r = fl.global_run(actionName, input);
        if (!r.state()) {
            return r;
        }
        return fl.runFor(actionName, input);
    }

    public RpcBefore filter(String actionName, FilterCallback fn) {
        String clsName = this.getClass().getSimpleName();
        FilterLink fl = filterArray.get(clsName);
        if (fl == null) {
            fl = FilterLink.build();
        }
        // 锁定过滤器，不允许新增了
        if (fl.isLocked()) {
            return this;
        }
        fl.put(actionName, fn);
        filterArray.put(clsName, fl);
        return this;
    }

    public RpcBefore filter(String[] actionNameArray, FilterCallback fn) {
        for (String actionName : actionNameArray) {
            filter(actionName, fn);
        }
        return this;
    }

    public RpcBefore lock() {
        String clsName = this.getClass().getSimpleName();
        FilterLink fl = filterArray.get(clsName);
        if (fl != null) {
            fl.lock();
        }
        return this;
    }

    public RpcBefore unlock() {
        String clsName = this.getClass().getSimpleName();
        FilterLink fl = filterArray.get(clsName);
        if (fl != null) {
            fl.unlock();
        }
        return this;
    }

    public RpcBefore input(ModelFilterCallback callback) {
        filter("insert", (func, paramArr) -> callback.run((JSONObject) paramArr[0], false))
                .filter("update", (func, paramArr) -> callback.run((JSONObject) paramArr[1], true));
        return this;
    }

    public RpcBefore delete(ModelIdsFilterCallback callback) {
        filter("delete", (func, paramArr) -> callback.run(((String) paramArr[0]).split(",")));
        return this;
    }
}
