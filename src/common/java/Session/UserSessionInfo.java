package common.java.Session;

import common.java.Apps.AppContext;
import common.java.Apps.Roles.AppRolesDef;
import common.java.Http.Server.HttpContext;
import common.java.ServiceTemplate.SuperItemField;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONObject;

public class UserSessionInfo {
    private final String sid;
    private final String uid;   // userName
    private final JSONObject userInfo;
    private final int expireTime = 1800;

    private UserSessionInfo(String sid, String uid, JSONObject userInfo) {
        this.sid = sid;
        this.uid = uid;
        this.userInfo = userInfo;
    }

    public static UserSessionInfo build(String sid, String uid, JSONObject userInfo) {
        return new UserSessionInfo(sid, uid, userInfo);
    }

    public static UserSessionInfo build(JSONObject fullUserInfo) {
        String sid = fullUserInfo.getString("_GrapeFW_SID");
        String uid = fullUserInfo.getString("_GrapeFW_UID");
        return new UserSessionInfo(sid, uid, fullUserInfo);
    }

    public String getSid() {
        return sid;
    }

    public String getUid() {
        return uid;
    }

    public int getAppId() {
        return userInfo.getInt(uid + "_GrapeFW_AppInfo_");
    }

    public int getExpireTime() {
        return userInfo.getInt("_GrapeFW_Expire");
    }

    public String getGroupId() {
        return userInfo.getString(SuperItemField.fatherField);
    }

    public int getGroupWeight() {
        return userInfo.getInt(SuperItemField.PVField);
    }

    public int getAdminLevel() {
        return userInfo.getInt(SuperItemField.AdminLevelField);
    }

    public JSONObject getUserInfo() {
        return userInfo;
    }

    private JSONObject append() {
        return userInfo.put("_GrapeFW_SID", sid)
                .put("_GrapeFW_UID", uid)
                .put("_GrapeFW_Expire", expireTime)
                .put("_GrapeFW_NeedRefresh", (expireTime + TimeHelper.build().nowSecond()) / 2)
                .put(uid + "_GrapeFW_AppInfo_", HttpContext.current().appId());
    }

    public JSONObject toEveryone() {
        userInfo.put(SuperItemField.fatherField, AppRolesDef.everyone.name)
                .put(SuperItemField.PVField, AppRolesDef.everyone.group_value)
                .put(SuperItemField.AdminLevelField, AppRolesDef.everyone.admin);
        return append();
    }

    public JSONObject toUser() {
        String grpName = userInfo.getString(SuperItemField.fatherField);  // 获得角色名称
        if (StringHelper.isInvalided(grpName)) {
            nLogger.errorInfo("当前用户[" + uid + "]未包含[" + SuperItemField.fatherField + "] ->字段信息,角色定义缺失!");
        }
        var roles = AppContext.current().roles();
        userInfo.put(SuperItemField.PVField, roles.getPV(grpName))
                .put(SuperItemField.AdminLevelField, roles.getAdminLevel(grpName));
        return append();
    }
}
