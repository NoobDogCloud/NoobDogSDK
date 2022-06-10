package common.java.Apps.MicroService.Model;

import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class MicroModelArray {
    private final HashMap<String, MicroModel> mModels;

    public MicroModelArray(String appId, String name, JSONObject mModel) {
        this.mModels = new HashMap<>();
        if (mModel != null) {
            for (String key : mModel.keySet()) {
                String _key = name + "_" + key.toLowerCase();
                this.mModels.put(_key, new MicroModel(appId, _key, mModel.getJson(_key)));
            }
        }
    }

    /**
     * 获得模型对象
     */
    public MicroModel microModel(String modelName) {
        return this.mModels.get(modelName);
    }

    /**
     * 获得全部模型对象
     */
    public HashMap<String, MicroModel> microModel() {
        return this.mModels;
    }

    /**
     * 获得JSON结构数据模型对象
     */
    public JSONObject toJson() {
        JSONObject rJson = new JSONObject();
        for (String key : this.mModels.keySet()) {
            rJson.put(key, this.mModels.get(key).toJson());
        }
        return rJson;
    }

    /**
     * 遍历全部数据
     */
    public void forEach(BiConsumer<String, MicroModel> lambdal) {
        for (String key : this.mModels.keySet()) {
            lambdal.accept(key, this.mModels.get(key));
        }
    }

}
