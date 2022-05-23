package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

public class AppRolesDef {
    public final static int RootLevel = 99;
    public final static int AdminLevel = 1;
    public final static int UserLevel = 0;

    public final static Role root = Role.build("root", "超级管理员", 10000000, null, RootLevel);
    public final static Role admin = Role.build("admin", "管理员", 1000000, "root", AdminLevel);
    public final static Role user = Role.build("user", "普通用户", 100000, "admin", UserLevel);
    public final static Role everyone = Role.build("everyone", "访客", 0, null, UserLevel);

    public static JSONObject defaultRoles() {
        return JSONObject.build().put(root.name, root.toRoleBlock())
                .put(admin.name, admin.toRoleBlock())
                .put(user.name, user.toRoleBlock())
                .put(everyone.name, everyone.toRoleBlock());
    }
}
