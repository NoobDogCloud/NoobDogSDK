package common.java.Apps.MicroService.Model.Interface;

import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MModelApiPerm {
    private final HashMap<String, String> permInfo;

    public MModelApiPerm(JSONObject apiPerm) {
        permInfo = apiPerm == null ?
                new HashMap<>() :
                apiPerm.toHashMap();
    }

    public List<Integer> getPerm(String apiName) {
        var str = permInfo.get(apiName);
        if (str == null) {
            return null;
        }
        String[] strArr = str.split(",");
        List<Integer> r = new ArrayList<>(strArr.length);
        for (String s : strArr) {
            r.add(Integer.parseInt(s));
        }
        return r;
    }

    public HashMap<String, String> getPermArray() {
        return permInfo;
    }
}
