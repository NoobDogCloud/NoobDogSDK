package common.java.Rpc;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class RpcFilterFnCache {
    private MethodAccess m;
    private Object o;
    private String className;

    private RpcFilterFnCache(Class<?> cls) {
        try {
            Constructor<?> co = cls.getDeclaredConstructor(null);
            this.o = co.newInstance(null);
            this.m = MethodAccess.get(cls);
            this.className = cls.getSimpleName();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RpcFilterFnCache build(Class<?> cls) {
        return new RpcFilterFnCache(cls);
    }

    public FilterReturn filter(String functionName, Object[] objs) {
        return (FilterReturn) m.invoke(o, "filter", className, functionName, objs);
    }

    public FilterReturn filter(String functionName, Object[] parameter, Object obj) {
        return (FilterReturn) m.invoke(o, "filter", className, functionName, parameter, obj);
    }

}
