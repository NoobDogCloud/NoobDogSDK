package common.java.Apps.MicroService.Model.Group;

import common.java.HttpServer.HttpContext;
import org.json.gsc.JSONObject;

public class MMGroupBlock {
    private final JSONObject nodeInfo;

    private MMGroupBlock(JSONObject nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

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
