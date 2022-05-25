package common.java.Session;

import org.json.gsc.JSONObject;

public interface UserSessionLayer {
    String save(UserSessionInfo userInfo, int expire);

    JSONObject getInfo(String uid);

    String getUID(String sid);

    boolean has(String sid);

    void delete(String uid, String sid);

    String update(UserSessionInfo userInfo, int expireTime);

    UserSession createSession(String uid, JSONObject info, int expire);
}
