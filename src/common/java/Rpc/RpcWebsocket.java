package common.java.Rpc;

import common.java.HttpServer.HttpContext;
import org.json.gsc.JSONObject;

public class RpcWebsocket {
    private final String token;
    private final String val;

    private RpcWebsocket(String token, String val) {
        this.token = token;
        this.val = val;
    }

    public static RpcWebsocket build(String token, String val) {
        return new RpcWebsocket(token, val);
    }

    public String toString() {
        return JSONObject.build().put(HttpContext.GrapeHttpHeader.WebSocket.wsId, token).put("record", val).toString();
    }
}
