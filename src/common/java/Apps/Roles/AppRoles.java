package common.java.Apps.Roles;

import org.json.gsc.JSONObject;

import java.util.*;

public class AppRoles {
    private final JSONObject store;

    private AppRoles(JSONObject info) {
        // 如果应用未配置角色,使用缺省角色,否则合并角色
        JSONObject defaultRoleArr = buildDefaultRoles();
        this.store = JSONObject.isInvalided(info) ? defaultRoleArr : info.put(defaultRoleArr);
        // this.updateMaxAndMin();
    }

    public static AppRoles build(JSONObject info) {
        return new AppRoles(info);
    }

    private JSONObject buildDefaultRoles() {
        return AppRolesDef.defaultRoles();
    }

    private JSONObject getRoleBlock(String roleName) {
        return store.containsKey(roleName) ? store.getJson(roleName) : null;
    }

    /**
     * 获得角色权值
     */
    public int getPV(String roleName) {
        var r = getRoleBlock(roleName);
        return JSONObject.isInvalided(r) ? -1 : r.getInt("weight");
    }

    /**
     * 获得角色父组
     */
    public String getElder(String roleName) {
        var r = getRoleBlock(roleName);
        if (r == null) {
            return null;
        }
        if (!r.containsKey("elder")) {
            return null;
        }
        return r.getString("elder");
    }

    /**
     * 获得权值大于 roleName 的全部角色名称
     */
    public List<String> gt(String roleName) {
        List<String> arr = new ArrayList<>();
        if (roleName != null) {
            int basePV = this.getPV(roleName);
            if (basePV >= 0) {
                for (String _roleName : store.keySet()) {
                    JSONObject block = store.getJson(_roleName);
                    if (block.getInt("weight") >= basePV) {
                        arr.add(_roleName);
                    }
                }
            }
        }
        return arr;
    }

    public List<String> gt(String roleName, List<String> _store) {
        List<String> arr = new ArrayList<>();
        if (roleName != null) {
            int basePV = this.getPV(roleName);
            if (basePV >= 0) {
                for (String _roleName : _store) {
                    JSONObject block = store.getJson(_roleName);
                    if (block.getInt("weight") >= basePV) {
                        arr.add(_roleName);
                    }
                }
            }
        }
        return arr;
    }

    /**
     * 获得权值小于 roleName 的全部角色名称
     */
    public List<String> lt(String roleName) {
        List<String> arr = new ArrayList<>();
        if (roleName != null) {
            int basePV = this.getPV(roleName);
            if (basePV >= 0) {
                for (String _roleName : store.keySet()) {
                    JSONObject block = store.getJson(_roleName);
                    if (block.getInt("weight") <= basePV) {
                        arr.add(_roleName);
                    }
                }
            }
        }
        return arr;
    }

    public List<String> lt(String roleName, List<String> _store) {
        List<String> arr = new ArrayList<>();
        if (roleName != null) {
            int basePV = this.getPV(roleName);
            if (basePV >= 0) {
                for (String _roleName : _store) {
                    JSONObject block = store.getJson(_roleName);
                    if (block.getInt("weight") <= basePV) {
                        arr.add(_roleName);
                    }
                }
            }
        }
        return arr;
    }

    /*
    private void updateMaxAndMin(){
        int temp_max = Integer.MIN_VALUE;
        String temp_max_name = null;
        int temp_min = Integer.MAX_VALUE;
        String temp_min_name = null;
        int current_val = 0;
        for (String _roleName : store.keySet()) {
            current_val = store.getInt(_roleName);
            if( current_val > temp_max){
                temp_max = current_val;
                temp_max_name = _roleName;
            }
            if( current_val < temp_min){
                temp_min = current_val;
                temp_min_name = _roleName;
            }
        }
        this.maxRole = temp_max_name;
        this.minRole = temp_min_name;
    }
    */

    /**
     * 根据当前权限 用户组， 生成新 有关角色组
     * 依赖原始的有序角色组列表，根据角色从属权构造有序有效角色列表
     */
    private Set<String> getRolesElder(Set<String> elderArr) {
        Set<String> parentSet = new HashSet<>();
        // 先根据权限角色组和其父角色，构造组
        for (String roleName : elderArr) {
            JSONObject block = store.getJson(roleName);
            if (block.containsKey("elder")) {
                String[] elderStrArr = block.getString("elder").split(",");
                parentSet.addAll(Arrays.asList(elderStrArr));
            }
        }
        if (parentSet.size() > 0) {
            elderArr.addAll(getRolesElder(parentSet));
        }
        return elderArr;
    }

    // 基于用户组从属组，按照权值重新构造新的用户组
    public List<String> getRolesTree(List<String> values) {
        Set<String> elderArr = new HashSet<>(values);
        getRolesElder(elderArr);
        // 按照用户组权值顺序，基于所有用到的用户组重拍
        List<String> elderGroup = new ArrayList<>();
        for (String roleName : store.keySet()) {
            if (elderArr.contains(roleName)) {
                elderGroup.add(roleName);
            }
        }
        return elderGroup;
    }

    public String getMaxRole(List<String> values) {
        return values.size() > 0 ? values.get(values.size() - 1) : null;
    }

    public String getMinRole(List<String> values) {
        return values.size() > 0 ? values.get(0) : null;
    }
}
