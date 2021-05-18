package common.java.Authority;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.MicroService.Model.RBAC.MModelPerm;
import common.java.Apps.MicroService.Model.RBAC.MModelPermInfo;
import common.java.Apps.Roles.AppRoles;
import common.java.Database.DbFilter;
import common.java.ServiceTemplate.SuperItemField;
import common.java.Session.UserSession;
import org.json.gsc.JSONObject;

import java.util.ArrayList;
import java.util.List;

/*
 * 权限模式使用，需要保证用户登录后，将用户个人信息，用户组信息都填充到位
 * */
public class Permissions {
    private final MModelPerm perms;

    public Permissions(String tableName) {
        this.perms = MicroServiceContext.current().model(tableName).perms();
    }

    /**
     * 根据权限逻辑定义，生成操作有关用户组
     */
    private List<String> groupArr(MModelPermInfo perm) {
        AppRoles roles = AppContext.current().roles();
        List<String> totalGroupArr = roles.getRolesTree(perm.value());
        switch (perm.logic()) {
            case MModelPermDef.perm_group_logic_gt:
                // 获得最小的用户组
                return roles.gt(roles.getMinRole(totalGroupArr), totalGroupArr);
            case MModelPermDef.perm_group_logic_lt:
                // 获得最大的用户组
                return roles.lt(roles.getMaxRole(totalGroupArr), totalGroupArr);
            case MModelPermDef.perm_group_logic_eq:
                return totalGroupArr;
            default:
                return new ArrayList<>();
        }
    }

    private boolean queryFilter(DbFilter dbf, MModelPermInfo perm) {
        if (perm == null) {        // 当前操作未定义权限,操作不限
            return true;
        }
        UserSession se = UserSession.current();
        if (!se.checkSession()) {  // 当前定义了权限,但是用户未登录
            se = UserSession.buildEveryone();
        }

        switch (perm.type()) {
            case MModelPermDef.perm_type_user:
                dbf.and().eq(SuperItemField.userIdField, se.getUID());
                break;
            case MModelPermDef.perm_type_group:
                var grpArr = this.groupArr(perm);
                for (var grpName : grpArr) {
                    dbf.or().eq(SuperItemField.groupIdField, grpName);
                }
                break;
            default:
                return false;
        }

        return true;
    }

    private void _completeFilter(JSONObject data, UserSession se, MModelPermInfo perm) {
        // 替换对应值
        switch (perm.type()) {
            case MModelPermDef.perm_type_user:
                data.put(SuperItemField.userIdField, se.getUID());
                break;
            case MModelPermDef.perm_type_group:
                data.put(SuperItemField.groupIdField, se.getGID())
                        .put(SuperItemField.PVField, se.getGPV());
                break;
        }
        // 无脑写入用户，用户组，用户组权限
        /*
        data.put(SuperItemField.userIdField, se.getUID())
                .put(SuperItemField.groupIdField, se.getGID())
                .put(SuperItemField.PVField, se.getGPV());
         */
    }

    private boolean completeFilter(JSONObject data, MModelPermInfo perm) {
        if (perm == null) {        // 当前操作未定义权限,操作不限
            return true;
        }
        UserSession se = UserSession.current();
        if (!se.checkSession()) {  // 当前定义了权限,但是用户未登录
            se = UserSession.buildEveryone();
        }
        _completeFilter(data, se, perm);
        return true;
    }

    // 读操作,增加过滤条件
    public boolean readFilter(DbFilter dbf) {
        return isAdmin() || queryFilter(dbf, perms.readPerm());
    }

    // 写操作,补充完善字段
    public boolean writeFilter(List<JSONObject> data) {
        if (isAdmin()) {
            return true;
        }
        MModelPermInfo perm = perms.createPerm();
        if (perm == null) {        // 当前操作未定义权限,操作不限
            return true;
        }
        UserSession se = UserSession.current();
        if (!se.checkSession()) {  // 当前定义了权限,但是用户未登录
            se = UserSession.buildEveryone();
        }
        // 判断是否有新增权限
        if (perm.type() != MModelPermDef.perm_type_group) {     // 新增类型必须是用户组
            return false;
        }
        // 判断当前用户组是否在允许组内
        if (!this.groupArr(perm).contains(se.getGID())) {
            return false;
        }
        for (JSONObject info : data) {
            _completeFilter(info, se, perm);
        }
        return true;
    }

    public boolean writeFilter(JSONObject data) {
        List<JSONObject> list = new ArrayList<>();
        list.add(data);
        return writeFilter(list);
    }

    // 删操作,增加过滤条件
    public boolean deleteFilter(DbFilter dbf) {
        return isAdmin() || queryFilter(dbf, perms.deletePerm());
    }

    // 改操作,补充完善字段和增加过滤条件
    public boolean updateFilter(DbFilter dbf, JSONObject data) {
        MModelPermInfo perm = perms.updatePerm();
        return isAdmin() || (queryFilter(dbf, perm) && completeFilter(data, perm));
    }

    // 是否是管理员
    private boolean isAdmin() {
        MModelPermInfo perm = perms.adminPerm();
        if (perm == null) {        // 当前操作未定义权限,未定义管理员
            return false;
        }
        UserSession se = UserSession.current();
        if (!se.checkSession()) {  // 当前定义了管理员,但是用户未登录
            se = UserSession.buildEveryone();
        }
        switch (perm.type()) {
            case MModelPermDef.perm_type_user:
                return perm.value().contains(se.getUID());  // 当前用户id包含在管理员用户组里
            case MModelPermDef.perm_type_group:
                this.groupArr(perm).contains(se.getGID());  // 当前用户组id包含在管理员组里
                break;
            default:
                return false;
        }
        return false;
    }
}
