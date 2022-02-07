package common.java.Reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MethodAccess {
    private final Class<?> cls;
    private final HashMap<String, List<MethodMap>> clsMethodMap = new HashMap<>();

    private MethodAccess(Class<?> cls) {
        this.cls = cls;
        for (Method m : cls.getMethods()) {
            var arr = clsMethodMap.getOrDefault(m.getName(), new ArrayList<>());
            arr.add(new MethodMap(m.getParameterTypes(), m));
            clsMethodMap.put(m.getName(), arr);
        }
    }

    public static MethodAccess get(Class<?> cls) {
        return new MethodAccess(cls);
    }

    public Object invoke(Object ins, String functionName, Object... Parameters) {
        var pArr = clsMethodMap.get(functionName);
        if (pArr == null) {
            return null;
        }

        int nearIdx = -1;
        Method nearMethod = null;
        for (var map : pArr) {
            var pa = map.paramClassArr();
            int l = pa.length;
            int pl = Parameters == null ? 0 : Parameters.length;
            if (l != pl)
                continue;
            // 0 参数方法
            if (l > 0) {
                int _nearIdx = -1;
                for (int i = 0; i < l; i++) {
                    // 输入参数为空，但是列表有数据，直接退出
                    if (Parameters == null) {
                        break;
                    }
                    // 空参数
                    if (Parameters[i] == null) {
                        continue;
                    }
                    // 任意类型 或者 类型不正确
                    if (!pa[i].equals(Object.class) && !pa[i].equals(Parameters[i].getClass())) {
                        continue;
                    }
                    // 获得参数列表最接近的
                    if (i > _nearIdx) {
                        _nearIdx++;
                        nearMethod = map.method();
                    }
                }
                if (_nearIdx == l) {
                    break;
                }
                if (_nearIdx > nearIdx) {
                    nearIdx = _nearIdx;
                }
            } else {
                nearMethod = map.method();
                break;
            }
        }

        if (nearMethod == null) {
            return null;
        }

        try {
            return nearMethod.invoke(ins, Parameters);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
