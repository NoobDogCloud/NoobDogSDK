package common.java.InterfaceModel;

import common.java.Database.IDBLayer;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.function.Function;

public interface IServiceDBLayer<T> extends IDBLayer<T> {
    T addFieldOutPipe(String fieldName, Function<Object, Object> func);

    T addFieldInPipe(String fieldName, Function<Object, Object> func);

    JSONArray selectByCache(int second);

    void invalidCache();

    JSONObject findByCache(int second);

    JSONObject findByCache();

    boolean sub(String fieldName, long num);

    JSONObject getAndSub(String fieldName, long num);

}
