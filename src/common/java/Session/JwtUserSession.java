package common.java.Session;

import common.java.Jwt.Jwt;
import common.java.Jwt.JwtInfo;
import org.json.gsc.JSONObject;

public class JwtUserSession implements UserSessionLayer {
    public JwtUserSession() {
    }

    public UserSession createSession(String uid, JSONObject info, int expire) {
        String sid = Jwt.createJwt(uid, info, expire);
        return UserSession.build(sid, expire);
    }

    public String save(UserSessionInfo userInfo, int expire) {
        return Jwt.createJwt(userInfo.getUid(), userInfo.getUserInfo(), expire);
    }

    public String update(UserSessionInfo userInfo, int expire) {
        return Jwt.createJwt(userInfo.getUid(), userInfo.getUserInfo(), expire);
    }

    public JSONObject getInfo(String fullSid) {
        return JwtInfo.buildBy(fullSid).decodeJwt();
    }

    public String getUID(String fullSid) {
        return JwtInfo.buildBy(fullSid).getUserName();
    }

    public boolean has(String fullSid) {
        return Jwt.isJwt(fullSid);
    }

    public void delete(String uid, String sid) {
        // 啥也不干
    }
}
