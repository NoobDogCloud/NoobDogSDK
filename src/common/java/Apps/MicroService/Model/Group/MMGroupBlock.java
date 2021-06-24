package common.java.Apps.MicroService.Model.Group;

import common.java.Http.Server.HttpContext;
import org.json.gsc.JSONObject;

public record MMGroupBlock(JSONObject nodeInfo) {

    public static MMGroupBlock build(JSONObject nodeInfo) {
        return new MMGroupBlock(nodeInfo);
    }

    public String service() {
        return nodeInfo.containsKey("service") ? nodeInfo.getString("service") : HttpContext.current().serviceName();
    }

    public String item() {
        return nodeInfo.getString("item");
    }

    public String key() {
        return nodeInfo.getString("key");
    }
}
