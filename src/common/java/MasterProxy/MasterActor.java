package common.java.MasterProxy;

import common.java.Config.Config;
import common.java.Coordination.Coordination;
import common.java.Rpc.RpcSubClient;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class MasterActor {
    private static final ConcurrentHashMap<String, MasterActor> master_actors = new ConcurrentHashMap<>();
    private final String actionName;
    private final Coordination coordination = Coordination.getInstance();

    private MasterActor(String actionName) {
        this.actionName = actionName;
    }


    public static MasterActor getInstance(String actionName) {
        if (!master_actors.containsKey(actionName)) {
            master_actors.put(actionName, new MasterActor(actionName));
        }
        return master_actors.get(actionName);
    }

    // 获得订阅管理客户端对象
    public static RpcSubClient getClient() {
        String ws_url = "ws://" + Config.masterHost + ":" + Config.masterPort;
        return RpcSubClient.build(ws_url);
    }

    public String getActionName() {
        return actionName;
    }

    // 根据 管理数据分类 获得对应全部数据
    public JSONArray<JSONObject> getData() {
        return coordination.getData(actionName);
    }

    // 获得当前分类 key的值 = val 的全部数据
    public JSONArray<JSONObject> getDataByIndex(String key, Object value) {
        return coordination.getData(actionName).filter(key, v -> v == value);
    }

}
