package common.java.Rpc;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;

public class RpcAfter {
    // 过滤链
    public static final HashMap<String, ReturnLink> filterArray = new HashMap<>();

    public static Object filter(String clsName, String actionName, Object[] parameter, Object returnValue) {
        ReturnLink rl = filterArray.get(clsName);
        if (rl == null) {
            return returnValue;
        }
        return rl.runFor(actionName, parameter, returnValue);
    }

    public void filter(String[] actionNameArray, Object[] parameter, ReturnCallback fn) {
        for (String actionName : actionNameArray) {
            filter(this.getClass().getSimpleName(), actionName, parameter, fn);
        }
    }

    public RpcAfter filter(String[] actionName, ReturnCallback fn) {
        for (String func : actionName) {
            filter(func, fn);
        }
        return this;
    }

    public RpcAfter filter(String actionName, ReturnCallback fn) {
        String clsName = this.getClass().getSimpleName();
        ReturnLink rl = filterArray.get(clsName);
        if (rl == null) {
            rl = ReturnLink.build();
        }
        if (rl.isLocked()) {
            return this;
        }
        rl.put(actionName, fn);
        filterArray.put(clsName, rl);
        return this;
    }

    public RpcAfter lock() {
        String clsName = this.getClass().getSimpleName();
        ReturnLink rl = filterArray.get(clsName);
        if (rl != null) {
            rl.lock();
        }
        return this;
    }

    public RpcAfter unlock() {
        String clsName = this.getClass().getSimpleName();
        ReturnLink rl = filterArray.get(clsName);
        if (rl != null) {
            rl.unlock();
        }
        return this;
    }

    public RpcAfter output(ModelJsonArrayReturnCallback callback) {
        filter("select", (funcName, parameter, returnValue) -> callback.run((JSONArray<JSONObject>) returnValue));
        return this;
    }

    public RpcAfter output(ModelJsonReturnCallback callback) {
        filter("find", (funcName, parameter, returnValue) -> callback.run((JSONObject) returnValue));
        return this;
    }

    public RpcAfter output(ModelPageReturnCallback callback) {
        filter("page,pageEx", (funcName, parameter, returnValue) -> callback.run((RpcPageInfo) returnValue));
        return this;
    }

    public RpcAfter input(ModelInputCallback callback) {
        filter("insert", (func, parameter, returnValue) -> callback.run(returnValue, (JSONObject) parameter[0]))
                .filter("update", (func, parameter, returnValue) -> callback.run(returnValue, (JSONObject) parameter[1]));
        return this;
    }

    public RpcAfter create(ModelInputCallback callback) {
        filter("insert", (func, parameter, returnValue) -> callback.run(returnValue, (JSONObject) parameter[0]));
        return this;
    }

    public RpcAfter update(ModelUpdateCallback callback) {
        filter("update", (func, parameter, returnValue) -> callback.run(returnValue, ((String) parameter[0]).split(","), (JSONObject) parameter[1]));
        return this;
    }

    public RpcAfter delete(ModelIdsFilterCallback callback) {
        filter("delete", (func, parameter, returnValue) -> callback.run(((String) parameter[0]).split(",")));
        return this;
    }
}
