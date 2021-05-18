package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

public class AppRolesDef {
    public final static Role root = Role.build("root", 10000000, null);
    public final static Role admin = Role.build("admin", 1000000, "root");
    public final static Role user = Role.build("user", 100000, "admin");
    public final static Role everyone = Role.build("everyone", 0, null);

    public static JSONObject defaultRoles() {
        return JSONObject.build().put(root.name, root.toRoleBlock())
                .put(admin.name, admin.toRoleBlock())
                .put(user.name, user.toRoleBlock())
                .put(everyone.name, everyone.toRoleBlock());
    }
}
