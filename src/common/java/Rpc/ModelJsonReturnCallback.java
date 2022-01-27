package common.java.Rpc;

import org.json.gsc.JSONObject;

// 返回的模型数据 Json 过滤器
@FunctionalInterface
public interface ModelJsonReturnCallback {
    JSONObject run(JSONObject returnValue);
}
