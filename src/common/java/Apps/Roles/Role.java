package common.java.Apps.Roles;

import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

public class Role {
    public final String name;
    public final String text;
    public final int group_value;
    public final String[] elder;
    public final int admin;

    private Role(String name, String text, int group_value, String elderArr, int admin) {
        this.name = name;
        this.text = text;
        this.group_value = group_value;
        this.elder = elderArr != null ? elderArr.split(",") : null;
        this.admin = admin;
    }

    public static Role build(String name, int group_value, String elderArr) {
        return new Role(name, "", group_value, elderArr, 0);
    }

    public static Role build(String name, String text, int group_value, String elderArr) {
        return new Role(name, text, group_value, elderArr, 0);
    }

    public static Role build(String name, String text, int group_value, String elderArr, int admin) {
        return new Role(name, text, group_value, elderArr, admin);
    }

    public static Role build(String name) {
        return new Role(name, "", 0, null, 0);
    }

    public int compareTo(Role r) {
        return this.group_value - r.group_value;
    }

    public String toString() {
        return this.name;
    }

    public JSONObject toRoleBlock() {
        var r = JSONObject.build("weight", group_value).put("admin_level", admin);
        if (elder != null) {
            r.put("elder", StringHelper.join(elder));
        }
        return r;
    }
}
