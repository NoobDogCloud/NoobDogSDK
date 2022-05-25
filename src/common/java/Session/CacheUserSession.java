package common.java.Session;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.Config.ModelServiceConfig;
import common.java.Cache.CacheHelper;
import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

import java.util.Objects;
import java.util.UUID;

public class CacheUserSession implements UserSessionLayer {
    private final CacheHelper cacher;

    public CacheUserSession() {
        cacher = getCacher();
    }

    public static CacheHelper getCacher() {
        ModelServiceConfig info = AppContext.current().config();
        if (info == null) {
            return null;
        }
        String appCache = info.cache();
        /*
        if (appCache == null) {
            nLogger.logInfo("应用[" + AppContext.current().appid() + "] 未设置缓存配置,无法使用会话系统!");
        }
        */
        return CacheHelper.build(appCache);
    }

    private static String UUID_Key(String uid, String fixed) {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        // 去掉"-"符号
        String temp = str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(19, 23) + str.substring(24);
        temp = fixed + temp + uid;
        return temp;
    }

    public UserSession createSession(String uid, JSONObject info, int expire) {
        String sid = buildUniqueID(uid);
        var userInfo = UserSessionInfo.build(sid, uid, info);
        // 先获得上次的会话实体ID并删除
        JSONObject lastInfo = cacher.getJson(uid);
        if (lastInfo != null) {
            String lastSID = UserSessionInfo.build(lastInfo).getSid();
            if (lastSID != null) {
                cacher.delete(lastSID);
            }
        }
        // 更新本次会话
        cacher.set(uid, expire, userInfo.toUser());//更新用户数据集
        cacher.set(sid, expire, uid);
        return UserSession.build(sid, expire);
    }

    public String save(UserSessionInfo userInfo, int expire) {
        String sid = userInfo.getSid();
        String uid = userInfo.getUid();
        JSONObject lastInfo = cacher.getJson(uid);
        if (lastInfo != null) {
            String lastSID = UserSessionInfo.build(lastInfo).getSid();
            if (lastSID != null) {
                cacher.delete(lastSID);
            }
        }
        // 更新本次会话
        cacher.set(uid, expire, userInfo.toUser());//更新用户数据集
        cacher.set(sid, expire, uid);
        return sid;
    }

    public String update(UserSessionInfo userInfo, int expireTime) {
        String sid = userInfo.getSid();
        String uid = userInfo.getUid();
        cacher.set(sid, expireTime, uid);
        cacher.set(uid, expireTime, userInfo.toString());
        return sid;
    }

    public JSONObject getInfo(String uid) {
        return cacher.getJson(uid);
    }

    public String getUID(String sid) {
        return cacher.getString(sid);
    }

    public void delete(String uid, String sid) {
        if (uid != null) {//uuid存在，有效
            cacher.delete(uid);
        }
        if (sid != null) {
            cacher.delete(sid);
        }
    }

    public boolean has(String sid) {
        return cacher != null && !StringHelper.isInvalided((String) cacher.get(sid));
    }

    public String buildUniqueID(String uid) {
        String tempUUID;
        do {
            tempUUID = CacheUserSession.UUID_Key(uid, "gsc_");
        }
        while (Objects.requireNonNull(cacher).get(tempUUID) != null);
        return tempUUID;
    }
}
