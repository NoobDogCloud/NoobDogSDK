package common.java.Rpc;

import org.json.gsc.JSONArray;

public class rMsgResponse {
    private final rMsgJson info;

    private rMsgResponse() {
        info = rMsgJson.build();
    }

    public static rMsgResponse build() {
        return new rMsgResponse();
    }

    public RpcResponse netMSG(Object state, Object data) {
        return RpcResponse.build(info.netMSG(state, data));
    }

    public RpcResponse netMSG(Object data) {
        return RpcResponse.build(info.netMSG(data));
    }

    public RpcResponse netMSG(int state, String message, Object data) {
        return RpcResponse.build(info.netMSG(state, message, data));
    }

    public RpcPageInfo netPAGE(int idx, int max, long count, JSONArray record) {
        return RpcPageInfo.Instant(idx, max, count, record);
    }

    public RpcResponse netState(Object state) {
        return RpcResponse.build(info.netState(state));
    }
}
