package common.java.InterfaceModel.Type;

import org.json.gsc.JSONArray;

@FunctionalInterface
public interface Aggregation {
    /**
     * @param inputArray 需要修改的输入流
     * @param storeArray 需要合并的数据流
     * @return 聚合后的数据流
     */
    JSONArray run(JSONArray inputArray, JSONArray storeArray);
}
