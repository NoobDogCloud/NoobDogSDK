package common.java.Apps.Roles;

import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

public class Role {
    public final String name;
    public final String text;
    public final int group_value;
    public final String[] elder;

    private Role(String name, String text, int group_value, String elderArr) {
        this.name = name;
        this.text = text;
        this.group_value = group_value;
        this.elder = elderArr != null ? elderArr.split(",") : null;
    }

    public static Role build(String name, int group_value, String elderArr) {
        return new Role(name, "", group_value, elderArr);
    }

    public static Role build(String name, String text, int group_value, String elderArr) {
        return new Role(name, text, group_value, elderArr);
    }

    public static Role build(String name) {
        return new Role(name, "", 0, null);
    }

    public int compareTo(Role r) {
        return this.group_value - r.group_value;
    }

    public String toString() {
        return this.name;
    }

    public JSONObject toRoleBlock() {
        var r = JSONObject.build("weight", group_value);
        if (elder != null) {
            r.put("elder", StringHelper.join(elder));
        }
        return r;
    }
}
