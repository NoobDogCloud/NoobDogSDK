package common.java.Rpc;

import common.java.Http.Server.HttpContext;
import org.json.gsc.JSONObject;

public record RpcWebsocket(String token, Object val) {

    public static RpcWebsocket build(String token, Object val) {
        return new RpcWebsocket(token, val);
    }

    public String toString() {
        return JSONObject.build().put(HttpContext.GrapeHttpHeader.WebSocket.wsId, token).put("record", val).toString();
    }
}
