package common.java.Coordination.Client;

import common.java.Config.Config;
import common.java.Coordination.Client.Store.Store;
import common.java.Coordination.Common.GscCenterEvent;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.JGrapeSystem.SystemDefined;
import common.java.Thread.ThreadHelper;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class GscCenterClient {
    private final Store store;
    private final AtomicInteger loadCnt;
    private final String serviceKey;
    private TcpClient client;
    private boolean liveStatus;
    private boolean keepLived;


    private GscCenterClient() {
        this.serviceKey = Config.getServiceName();
        this.liveStatus = false;
        this.keepLived = false;
        this.loadCnt = new AtomicInteger(0);
        this.store = Store.build();
        this.client = TcpClient.build(this).run();
    }

    public static GscCenterClient build() {
        // 建立连接
        return new GscCenterClient();
    }

    public GscCenterClient setConnected(Consumer<GscCenterClient> fn) {
        this.client.setConnected(() -> fn.accept(this));
        return this;
    }

    public GscCenterClient reConnect() {
        if (this.keepLived) {
            this.client.close().run();
        }
        return this;
    }

    public boolean getLiveStatus() {
        return this.liveStatus;
    }

    public GscCenterClient setLiveStatus(boolean status) {
        this.liveStatus = status;
        return this;
    }

    public boolean getKeepLived() {
        return this.keepLived;
    }

    public GscCenterClient setKeepLived(boolean keepLived) {
        this.keepLived = keepLived;
        return this;
    }

    public void waitLived() {
        int err_no = 100;
        while (!this.liveStatus && err_no > 10) {
            ThreadHelper.sleep(100);
            err_no--;
        }
    }

    // 根据不同className获得对应数据
    public JSONArray<JSONObject> getData(String className, String key, Object value) {
        return store.find(className, key, value);
    }

    public JSONArray<JSONObject> getData(String className) {
        return store.find(className);
    }

    private void setClient(TcpClient handle) {
        this.client = handle;
    }

    // 获得等待
    public void getResponse() {
        if (loadCnt.get() > 0) {
            loadCnt.set(loadCnt.decrementAndGet());
        }
    }

    public void setResponse(int no) {
        loadCnt.addAndGet(no);
    }

    // 需要等待
    public void waitResponse() {
        int max_errno = 100;
        while (loadCnt.get() > 0 && max_errno > 0) {
            ThreadHelper.sleep(100);
            max_errno--;
        }
    }


    // 初始化数据
    public void onChange(String key, JSONObject data) {
        if (data.containsKey("data")) {
            store.put(key, data.getJsonArray("data"));
        }
    }

    public void onClear() {
        store.clear();
    }

    /**
     * @apiNote 订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient subscribe() {
        this.setResponse(3);
        client.send(GscCenterPacket.build(serviceKey,
                JSONObject.build("node", Config.nodeID).put("ip", Config.bindIP.equals("0.0.0.0") ? SystemDefined.ip() : Config.bindIP).put("port", Config.port),
                GscCenterEvent.Subscribe, false));
        this.waitResponse();
        return this;
    }

    /**
     * @apiNote 取消订阅挂载(订阅除了key外, 还要带入当前微服务名和节点ID)
     */
    public GscCenterClient unSubscribe() {
        client.send(GscCenterPacket.build(serviceKey, JSONObject.build("node", Config.nodeID), GscCenterEvent.UnSubscribe, false));
        return this;
    }

    public GscCenterClient disconnect() {
        client.send(GscCenterPacket.build(serviceKey, JSONObject.build("node", Config.nodeID), GscCenterEvent.TestDisconnect, false));
        return this;
    }

    public void close() {
        this.setKeepLived(false);
        client.close();
    }

}
