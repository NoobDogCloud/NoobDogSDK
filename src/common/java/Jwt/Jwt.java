package common.java.Jwt;

import org.json.gsc.JSONObject;

public class Jwt {
    public static String createJwt(String userName, JSONObject userInfo) {
        return JwtInfo.build(userName).encodeJwt(userInfo).toString();
    }
}
