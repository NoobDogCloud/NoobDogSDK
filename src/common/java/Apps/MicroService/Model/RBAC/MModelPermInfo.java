package common.java.Apps.MicroService.Model.RBAC;

import common.java.Apps.AppContext;
import common.java.Apps.Roles.Role;
import common.java.Authority.MModelPermDef;
import common.java.String.StringHelper;
import org.json.gsc.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * 权限节点设计,包含权限类型和权限值
 */

public class MModelPermInfo {
    private final JSONObject info;
    private List<String> group_values;

    private MModelPermInfo(JSONObject info) {
        this.info = info;
        String typeStr = info.getString(MModelPermDef.perm_type_caption);
        if ("user".equals(typeStr)) {
            info.put(MModelPermDef.perm_type_caption, MModelPermDef.perm_type_user);
        } else {
            info.put(MModelPermDef.perm_type_caption, MModelPermDef.perm_type_group);
        }
        // 是用户组类型权限
        if (info.getInt(MModelPermDef.perm_type_caption) == MModelPermDef.perm_type_group) {
            // 根据 用户组值 从小到大排序 用户组名
            updateSortRole();
        } else {
            group_values = null;
        }
    }

    public static MModelPermInfo build(JSONObject info) {
        return new MModelPermInfo(info);
    }

    public static MModelPermInfo build() {
        return new MModelPermInfo(new JSONObject());
    }

    // 按照用户组权值排序
    private void updateSortRole() {
        var appRoles = AppContext.current().roles();
        String[] roleArr = Arrays.stream(this.info.getString(MModelPermDef.perm_value_caption).split(","))
                .distinct()
                .map(v -> Role.build(v, appRoles.getPV(v), appRoles.getElder(v)))
                .sorted(Role::compareTo)
                .map(v -> v.name)
                .toArray(String[]::new);
        this.info.put(MModelPermDef.perm_value_caption, StringHelper.join(roleArr));
        this.group_values = Arrays.asList(roleArr);
    }

    /**
     * 获得权限类型
     */
    public int type() {
        return this.info.getInt(MModelPermDef.perm_type_caption);
    }

    /**
     * 获得权限逻辑(有可能为null)
     */
    public String logic() {
        return this.info.getString(MModelPermDef.perm_logic_caption);
    }

    /**
     * 获得记录值(管理员才有用)
     */
    public List<String> value() {
        // return this.info.getString(MModelPermDef.perm_value_caption).split(",");
        return group_values;
    }

    /**
     * 设置类型
     */
    public MModelPermInfo type(int t) {
        this.info.put(MModelPermDef.perm_type_caption, t);
        return this;
    }

    /**
     * 设置值
     */
    public MModelPermInfo value(String v) {
        this.info.put(MModelPermDef.perm_value_caption, v);
        updateSortRole();
        return this;
    }

    /**
     * 输出权限节点内容
     */
    public JSONObject toJson() {
        return this.info;
    }
}
