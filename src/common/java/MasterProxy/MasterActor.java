package common.java.MasterProxy;

import common.java.Config.Config;
import common.java.Coordination.Client.GscCenterClient;
import common.java.Http.Server.HttpContext;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.RpcResponse;
import kong.unirest.Unirest;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class MasterActor {
    private static final ConcurrentHashMap<String, MasterActor> master_actors = new ConcurrentHashMap<>();
    private static final GscCenterClient client = GscCenterClient.build();
    private final String actionName;

    private MasterActor(String actionName) {
        this.actionName = actionName;
    }

    private static RpcResponse rpc_secret(String className, String actionName, Object... parameter) {
        String v = ExecRequest.objects2string(parameter);
        return RpcResponse.build(Unirest.get("http://" + Config.masterHost + ":" + Config.masterManagerPort + "/system/" + className + "/" + actionName + v)
                .header(HttpContext.GrapeHttpHeader.publicKey, Config.publicKey)
                .asJson()
                .getBody()
                .getObject());
    }

    // 检查客户端网络是否正常,不正常重新连接
    private static GscCenterClient check() {
        if (!client.getLiveStatus()) {
            client.setKeepLived(true).reConnect().waitLived();
        }
        return client;
    }

    public static GscCenterClient getClient() {
        return check();
    }

    public static MasterActor getInstance(String actionName) {
        if (!master_actors.containsKey(actionName)) {
            master_actors.put(actionName, new MasterActor(actionName));
        }
        return master_actors.get(actionName);
    }

    // 新增idx
    public int insert(JSONObject info) {
        return rpc_secret(actionName, "insert", info).asInt();
    }

    public boolean update(int id, JSONObject info) {
        return rpc_secret(actionName, "update", id, info).status();
    }

    public JSONObject find(int id) {
        return rpc_secret(actionName, "find", id).asJson();
    }

    public JSONArray<JSONObject> select(String key, Object val) {
        return rpc_secret(actionName, "select", key, val).asJsonArray();
    }

    public boolean delete(int id) {
        return rpc_secret(actionName, "delete", id).status();
    }

    public JSONArray<JSONObject> getData() {
        return client.getData(actionName);
    }

    public String getActionName() {
        return actionName;
    }

    public JSONArray<JSONObject> getDataByIndex(String key, Object value) {
        return client.getData(actionName, key, value);
    }

}
