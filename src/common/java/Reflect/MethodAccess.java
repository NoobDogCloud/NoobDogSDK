package common.java.Reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MethodAccess {
    private static final Class<?>[][] sameClassTemplate = new Class<?>[][]{
            {Integer.class, int.class},
            {Long.class, long.class},
            {Float.class, float.class},
            {Double.class, double.class},
            {Boolean.class, boolean.class},
            {Byte.class, byte.class},
            {Character.class, char.class},
            {Short.class, short.class},
    };
    // private final Class<?> cls;
    private final HashMap<String, List<MethodMap>> clsMethodMap = new HashMap<>();

    private MethodAccess(Class<?> cls) {
        // this.cls = cls;
        for (Method m : cls.getMethods()) {
            var arr = clsMethodMap.getOrDefault(m.getName(), new ArrayList<>());
            arr.add(new MethodMap(m.getParameterTypes(), m));
            clsMethodMap.put(m.getName(), arr);
        }
    }

    public static MethodAccess get(Class<?> cls) {
        return new MethodAccess(cls);
    }

    // 比较输入参数类型 与 接口定义类型
    // 强制将 int/integer 等包装类型转换为原始类型
    public boolean parameterCompare(Class<?> declared, Class<?> actual) {
        // 从 sameClassTemplate 拿到符合 declared 类型对
        Class<?>[] classList = null;
        for (int i = 0, l = sameClassTemplate.length; i < l && classList == null; i++) {
            var arr = sameClassTemplate[i];
            for (var c : arr) {
                if (c.equals(declared)) {
                    classList = arr;
                    break;
                }
            }
        }
        // 如果没有找到符合的类型对
        if (classList == null) {
            return declared.equals(actual);
        }
        // 如果找到了符合的类型对
        for (var c : classList) {
            if (c.equals(actual)) {
                return true;
            }
        }
        return false;
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
                    // 空参数
                    if (Parameters[i] == null) {
                        continue;
                    }
                    // 任意类型 或者 类型不正确
                    // if (!pa[i].equals(Object.class) && !pa[i].equals(Parameters[i].getClass())) {
                    if (!pa[i].equals(Object.class) && !parameterCompare(pa[i], Parameters[i].getClass())) {
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
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
