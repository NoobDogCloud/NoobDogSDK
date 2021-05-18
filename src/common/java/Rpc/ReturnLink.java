package common.java.Rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReturnLink {
    private final HashMap<String, List<ReturnCallback>> filterArray;
    private ReturnCallback global_fn;
    private boolean global_lock;

    private ReturnLink() {
        filterArray = new HashMap<>();
        global_fn = null;
    }

    public static ReturnLink build() {
        return new ReturnLink();
    }

    public ReturnLink lock() {
        global_lock = true;
        return this;
    }

    public ReturnLink unlock() {
        global_lock = false;
        return this;
    }

    public boolean isLocked() {
        return global_lock;
    }

    public ReturnLink put(String actionName, ReturnCallback fn) {
        if (actionName.equals("*")) {
            global_fn = fn;
        } else {
            List<ReturnCallback> ar = filterArray.get(actionName);
            if (ar == null) {
                ar = new ArrayList<>();
            }
            ar.add(fn);
            filterArray.put(actionName, ar);
        }
        return this;
    }

    public Object global_run(String actionName, Object[] parameter, Object returnValue) {
        if (global_fn == null) {
            return FilterReturn.buildTrue();
        }
        return returnValue;
    }

    public Object runFor(String actionName, Object[] parameter, Object returnValue) {
        List<ReturnCallback> ar = filterArray.get(actionName);
        if (ar == null) {
            return returnValue;
        }
        for (ReturnCallback fn : ar) {
            returnValue = fn.run(actionName, parameter, returnValue);
        }
        return returnValue;
    }
}
