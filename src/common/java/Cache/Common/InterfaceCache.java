package common.java.Cache.Common;

import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface InterfaceCache {
    Object get(String objectName);

    int getInt(String objectName);

    long getLong(String objectName);

    float getFloat(String objectName);

    double getDouble(String objectName);

    boolean getBoolean(String objectName);

    String getString(String objectName);

    BigDecimal getBigDecimal(String objectName);

    BigInteger getBigInteger(String objectName);

    JSONObject getJson(String objectName);

    JSONArray getJsonArray(String objectName);

    boolean setExpire(String objectName, int expire);

    String set(String objectName, Object objectValue);

    String set(String objectName, int expire, Object objectValue);

    boolean setNX(String objectName, Object objectValue);

    Object getSet(String objectName, Object objectValue);

    Object getSet(String objectName, int expire, Object objectValue);

    long inc(String objectName);

    long incBy(String objectName, long num);

    long dec(String objectName);

    long decBy(String objectName, long num);

    long delete(String objectName);
}
