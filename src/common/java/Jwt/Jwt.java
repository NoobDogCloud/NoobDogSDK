package common.java.Jwt;

import org.json.gsc.JSONObject;

public class Jwt {
    public static String createJwt(String userName, JSONObject userInfo) {
        return createJwt(userName, userInfo, 0);
    }

    public static String createJwt(String userName, JSONObject userInfo, int expire) {
        var j = JwtInfo.build(userName).encodeJwt(userInfo);
        if (expire > 0) {
            j.setExpire(expire);
        }
        return j.toString();
    }

    public static boolean isJwt(String fullToken) {
        return fullToken.startsWith("Jwt_") && fullToken.endsWith("_");
    }
}
