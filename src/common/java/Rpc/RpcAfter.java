package common.java.Rpc;

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
}
