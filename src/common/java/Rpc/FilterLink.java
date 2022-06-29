package common.java.Rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilterLink {
    private final HashMap<String, List<FilterCallback>> filterArray;
    private FilterCallback global_fn;
    private boolean global_lock;

    private FilterLink() {
        filterArray = new HashMap<>();
        global_fn = null;
        global_lock = false;
    }

    public static FilterLink build() {
        return new FilterLink();
    }

    public FilterLink lock() {
        global_lock = true;
        return this;
    }

    public FilterLink unlock() {
        global_lock = false;
        return this;
    }

    public boolean isLocked() {
        return global_lock;
    }

    public FilterLink put(String actionName, FilterCallback fn) {
        if (actionName.equals("*")) {
            global_fn = fn;
        } else {
            List<FilterCallback> ar = filterArray.get(actionName);
            if (ar == null) {
                ar = new ArrayList<>();
            }
            ar.add(fn);
            filterArray.put(actionName, ar);
        }
        return this;
    }

    public FilterReturn global_run(String actionName, Object[] input) {
        if (global_fn == null) {
            return FilterReturn.success();
        }
        return global_fn.run(actionName, input);
    }

    public FilterReturn runFor(String actionName, Object[] input) {
        List<FilterCallback> ar = filterArray.get(actionName);
        if (ar == null) {
            return FilterReturn.success();
        }
        for (FilterCallback fn : ar) {
            FilterReturn fr = fn.run(actionName, input);
            if (!fr.state()) {  // 有执行失败的
                return fr;
            }
        }
        return FilterReturn.success();
    }
}
