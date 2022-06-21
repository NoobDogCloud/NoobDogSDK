package common.java.Apps.MicroService.Model.Interface;

import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MModelApiPerm {
    private final HashMap<String, Integer[]> permInfo = new HashMap<String, Integer[]>();

    public MModelApiPerm(JSONObject apiPerm) {
        if (apiPerm != null) {
            var h = apiPerm.toHashMap();
            for (var k : h.keySet()) {
                var v_arr = h.get(k).toString().split("#");
                var perm_arr = v_arr[0].split(",");
                var perm_arr_int = new Integer[perm_arr.length];
                for (var i = 0; i < perm_arr.length; i++) {
                    perm_arr_int[i] = Integer.parseInt(perm_arr[i]);
                }
                apiPerm.put(k, perm_arr_int);
            }
        }
    }

    public Integer[] getPerm(String apiName) {
        return permInfo.getOrDefault(apiName, null);
    }

    public HashMap<String, Integer[]> getPermArray() {
        return permInfo;
    }
}
