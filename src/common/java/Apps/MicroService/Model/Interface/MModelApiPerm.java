package common.java.Apps.MicroService.Model.Interface;

import org.json.gsc.JSONObject;

import java.util.HashMap;

public class MModelApiPerm {
    private final HashMap<String, Long> permInfo;

    public MModelApiPerm(JSONObject apiPerm) {
        permInfo = apiPerm == null ?
                new HashMap<>() :
                apiPerm.toHashMap();
    }

    public Long getPerm(String apiName) {
        return permInfo.getOrDefault(apiName, 0L);
    }

    public HashMap<String, Long> getPermArray() {
        return permInfo;
    }
}
