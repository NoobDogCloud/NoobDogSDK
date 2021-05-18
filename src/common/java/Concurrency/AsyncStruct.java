package common.java.Concurrency;

import common.java.Cache.CacheHelper;
import common.java.GscCommon.checkModel;
import org.json.gsc.JSONObject;

public class AsyncStruct {
    private final CacheHelper cache = CacheHelper.build();//使用当前服务的缓存
    private long currentNumber;
    private long totalNumber;
    private String currentText;
    private int state;
    private String queryKey;

    private AsyncStruct(long currentNumber, long totalNumber, String currentText, int state) {
        this.currentNumber = currentNumber;
        this.totalNumber = totalNumber == 0 ? 100 : totalNumber;
        this.currentText = currentText;
        this.state = state;
    }

    public static AsyncStruct init(String queryKey) {
        return build().bindQueryKey(queryKey);
    }

    public static AsyncStruct build() {
        return build(0);
    }

    public static AsyncStruct build(long totalNumber) {
        return new AsyncStruct(0, totalNumber, "", checkModel.pending);
    }

    public static AsyncStruct build(JSONObject info) {
        return new AsyncStruct(info.getLong("currentNumber"), info.getLong("totalNumber"), info.getString("currentText"), info.getInt("state"));
    }

    public long getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(long currentNumber) {
        this.currentNumber = currentNumber;
    }

    public long getTotalNumber() {
        return totalNumber;
    }

    public void setTotalNumber(long totalNumber) {
        this.totalNumber = totalNumber;
    }

    public String getCurrentText() {
        return currentText;
    }

    public void setCurrentText(String currentText) {
        this.currentText = currentText;
    }

    private AsyncStruct bindQueryKey(String queryKey) {
        this.queryKey = queryKey;
        JSONObject info = cache.getJson(queryKey);
        if (JSONObject.isInvalided(info)) {
            this.currentNumber = 0;
            this.totalNumber = 100;
            this.currentText = "";
            this.state = checkModel.pending;
        } else {
            this.currentNumber = info.getLong("currentNumber");
            this.totalNumber = info.getLong("totalNumber");
            this.currentText = info.getString("currentText");
            this.state = info.getInt("state");
        }
        return this;
    }

    public AsyncStruct success() {
        this.state = checkModel.success;
        return this;
    }

    public AsyncStruct fail() {
        this.state = checkModel.failed;
        return this;
    }

    public JSONObject toJson() {
        return JSONObject.build("currentNumber", this.currentNumber)
                .put("totalNumber", this.totalNumber)
                .put("currentText", this.currentText)
                .put("state", this.state);
    }

    public void save() {
        cache.set(queryKey, toJson().toString());
    }
}
