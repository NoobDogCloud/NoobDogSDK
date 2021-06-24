package common.java.Coordination;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

public class Coordination {
    private static Coordination handle;
    private final AtomicReference<JSONArray<JSONObject>> apps = new AtomicReference<>();
    private final AtomicReference<JSONArray<JSONObject>> services = new AtomicReference<>();
    private final AtomicReference<JSONArray<JSONObject>> configs = new AtomicReference<>();

    private Coordination() {
    }

    private Coordination(JSONObject data) {
        init(data);
    }

    public static Coordination build(JSONObject data) {
        if (handle == null) {
            handle = new Coordination(data);
        } else {
            handle.init(data);
        }
        return handle;
    }

    public static Coordination getInstance() {
        if (handle == null) {
            handle = new Coordination();
        }
        return handle;
    }

    private void init(JSONObject data) {
        apps.set(data.getJsonArray("apps"));
        services.set(data.getJsonArray("services"));
        configs.set(data.getJsonArray("configs"));
    }

    public JSONArray<JSONObject> getAppArray() {
        return apps.get();
    }

    public JSONArray<JSONObject> getServiceArray() {
        return services.get();
    }

    public JSONArray<JSONObject> getConfigArray() {
        return configs.get();
    }

    public JSONArray<JSONObject> getData(String className) {
        return switch (className) {
            case "apps" -> getAppArray();
            case "services" -> getServiceArray();
            case "configs" -> getConfigArray();
            default -> throw new IllegalStateException("Unexpected value: " + className);
        };
    }
}
