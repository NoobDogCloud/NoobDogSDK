package common.java.Jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import common.java.Encrypt.Md5;
import common.java.Time.TimeHelper;
import org.json.gsc.JSONObject;

import java.util.Date;
import java.util.HashMap;

public class JwtInfo {
    private final String userName;
    private String version = "v3";
    private String token;
    private String sign;

    private int expire = 86400 * 1000;

    public void setExpire(int expire) {
        this.expire = expire;
    }

    private JwtInfo(String userName) {
        this.userName = userName;
    }

    public static JwtInfo build(String userName) {
        return new JwtInfo(userName);
    }

    public static JwtInfo buildBy(String fullToken) {
        String[] metaArr = fullToken.split("_");
        if (metaArr.length != 4) {
            return null;
        }
        var r = new JwtInfo(metaArr[2]);
        r.token = metaArr[3];
        r.version = metaArr[1];
        r.sign = metaArr[0];
        return r;
    }

    public String getUserName() {
        return userName;
    }

    public String getToken() {
        return token;
    }

    public JwtInfo setToken(String token) {
        this.token = token;
        return this;
    }

    public String getSalt(String userName) {
        return Md5.build(userName + "salt") + "1239+x$!";
    }

    public String toString() {
        return "Jwt_" + version + "_" + userName + "_" + token + "_";
    }

    public boolean isValid() {
        return sign.equals("Jwt");
    }

    private static JSONObject filterSafe(JSONObject info) {
        info.entrySet().removeIf(v -> v.getValue() == null);
        return info;
    }

    public JwtInfo encodeJwt(JSONObject userInfo) {
        long ft = TimeHelper.build().nowMillis() + (86400 * 1000);
        userInfo.put("failure_time", ft);
        filterSafe(userInfo);
        token = JWT.create()
                .withHeader(new HashMap<>())
                .withClaim("user", userInfo)
                .withExpiresAt(new Date(ft))
                .sign(Algorithm.HMAC256(getSalt(userName)));
        return this;
    }

    public JSONObject decodeJwt() {
        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(getSalt(userName))).build();
            DecodedJWT decodedJWT = jwtVerifier.verify(token);
            return JSONObject.build(decodedJWT.getClaim("user").asMap());
        } catch (Exception e) {
            return null;
        }
    }
}
