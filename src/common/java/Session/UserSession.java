package common.java.Session;

import common.java.Apps.Roles.AppRolesDef;
import common.java.Cache.CacheHelper;
import common.java.Http.Server.HttpContext;
import common.java.Jwt.Jwt;
import common.java.Jwt.JwtInfo;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

public class UserSession {
    private static final int session_time = 86400;
    private static final String everyone_key = AppRolesDef.everyone.name;
    private UserSessionInfo sessionInfo;    //会话id控制
    private String uid;                //当前操作的用户名
    private String sid;                //当前操作的会话ID
    private String gid;                //当前操作的用户组ID
    private int gPV;                   // 当前操作用户的用户组权值
    private int adminLevel;            // 当前操作用户的管理员级别
    private int appid;                //当前会话所属APPID
    private int expireTime;

    private boolean jwtStatus = false;

    private UserSessionLayer layer;

    private UserSession() {
        // sid 可能是会话id，也可能是jwt加密信息
        String sid = getRequestSID();
        this.expireTime = 1800;
        updateUserInfo(sid);
    }

    //绑定会话
    private UserSession(String sid) {
        init(sid, -1);
    }

    private UserSession(String sid, int expireTime) {
        init(sid, expireTime);
    }

    public static UserSession current() {
        return new UserSession();
    }

    public static UserSession build(String sid) {
        return new UserSession(sid);
    }

    public static UserSession build(String sid, int expireTime) {
        return new UserSession(sid, expireTime);
    }

    public static UserSession buildEveryone() {
        return build(everyone_key);
    }

    public static boolean checkSession(String sid) {
        if (Jwt.isJwt(sid)) {
            return true;
        }
        CacheHelper ch = CacheUserSession.getCacher();
        return ch != null && !StringHelper.isInvalided((String) ch.get(sid));
    }

    public static boolean hasSession() {
        return UserSession.getRequestSID() != null;
    }

    /**
     * 获得当前会话id，如果不存在返回空
     */
    public static String getRequestSID() {
        Object temp;
        try {
            temp = HttpContext.current().sid();
        } catch (Exception e) {
            temp = null;
        }
        return temp == null || temp.equals("") ? null : temp.toString();
    }

    public static UserSession createSession(String uid, JSONObject info, int expire) {
        UserSessionLayer l = CacheUserSession.getCacher() != null ? new CacheUserSession() : new JwtUserSession();
        return l.createSession(uid, info, expire);
    }

    /**
     * 创建会话
     *
     * @param uid
     * @param json
     * @return
     */
    public static UserSession createSession(String uid, JSONObject json) {
        return createSession(uid, json, session_time);
    }

    /**
     * 创建临时会话
     *
     * @param code   临时会话id
     * @param expire 有效期(秒)
     * @return
     */
    public static UserSession createGuessSession(String code, JSONObject data, int expire) {
        return createSession(code + "#guesser", data);
    }

    /**
     * 创建临时会话
     *
     * @param code   临时会话id
     * @param expire 有效期(秒)
     * @return
     */
    public static UserSession createGuessSession(String code, int expire) {
        return createGuessSession(code, JSONObject.build(), expire);
    }

    private void init(String sid, int expireTime) {
        this.expireTime = expireTime;
        if (!updateUserInfo(sid)) {
            nLogger.logInfo("sid:" + sid + " ->无效");
        }
    }

    public boolean checkSession() {
        if (sid == null) {
            return false;
        }
        if (sid.equals(everyone_key)) {
            return true;
        }
        if (jwtStatus) {
            return JwtInfo.buildBy(sid).isValid();
        }
        return layer.has(sid);
    }

    /**
     * 替换会话数据
     *
     * @return
     */
    public UserSession setUserData(JSONObject newData) {
        sessionInfo.setUserInfo(newData);
        updateUserInfo(layer.save(sessionInfo, this.expireTime));
        return this;
    }

    public UserSessionInfo getUserData() {
        return sessionInfo;
    }

    /**
     * 根据sid删除会话
     */
    public void deleteSession() {
        layer.delete(uid, sid);
    }

    // 延续会话维持时间(20分钟)
    public UserSession refreshSession() {
        if (this.expireTime > 0) {
            JSONObject info = sessionInfo.getData();
            int need_expire_time = info.getInt("_GrapeFW_NeedRefresh");
            long t = TimeHelper.build().nowSecond() + expireTime;
            if (t < need_expire_time) {
                return this;
            }
            if (!this.sid.equals(everyone_key)) {
                info.put("_GrapeFW_NeedRefresh", t);
                updateUserInfo(layer.update(sessionInfo, expireTime));
            }
        }
        return this;
    }

    //更新当前会话有关信息（根据sid获得当前用户信息）
    private boolean updateUserInfo(String sid) {
        boolean rb = false;
        if (sid != null) {
            this.sid = sid;
            if (Jwt.isJwt(sid)) {
                jwtStatus = true;
                JwtInfo jwtInfo = JwtInfo.buildBy(sid);
                if (jwtInfo != null && jwtInfo.isValid()) {
                    uid = jwtInfo.getUserName();
                    sessionInfo = UserSessionInfo.build(jwtInfo.decodeJwt()).toUser();
                }
            } else {
                layer = new CacheUserSession();
                String uid = sid.equals(everyone_key) ? everyone_key : layer.getUID(sid);
                if (uid != null && !uid.isEmpty()) {//返回了用户名
                    this.uid = uid;
                    sessionInfo = sid.equals(everyone_key) ?
                            UserSessionInfo.build(sid, uid, JSONObject.build()).toEveryone()
                            : UserSessionInfo.build(sid, uid, layer.getInfo(uid)).toUser();
                }
            }
            // 补充会话数据
            if (sessionInfo != null) {
                this.appid = sessionInfo.getAppId();//获得所属appid
                this.expireTime = sessionInfo.getExpireTime();
                this.gid = sessionInfo.getGroupId();//获得所在组ID
                this.gPV = sessionInfo.getGroupWeight();//获得所在组权值
                this.adminLevel = sessionInfo.getAdminLevel();//获得管理员级别
                // 更新会话维持时间
                refreshSession();
                rb = true;
            }
        }
        return rb;
    }

    public String getSID() {
        return this.sid;
    }

    public String getUID() {
        return this.uid;
    }

    public String getGID() {
        return this.gid;
    }

    /**
     * 获得当前用户组权限值
     */
    public int getGPV() {
        return this.gPV;
    }

    public int getAppID() {
        return this.appid;
    }

    public int getAdminLevel() {
        return this.adminLevel;
    }
}