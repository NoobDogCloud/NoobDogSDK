package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

public class AppRolesDef {
    public final static Role root = Role.build("root", "超级管理员", 10000000, null);
    public final static Role admin = Role.build("admin", "管理员", 1000000, "root");
    public final static Role user = Role.build("user", "普通用户", 100000, "admin");
    public final static Role everyone = Role.build("everyone", "访客", 0, null);

    public static JSONObject defaultRoles() {
        return JSONObject.build().put(root.name, root.toRoleBlock())
                .put(admin.name, admin.toRoleBlock())
                .put(user.name, user.toRoleBlock())
                .put(everyone.name, everyone.toRoleBlock());
    }
}
