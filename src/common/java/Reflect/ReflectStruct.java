package common.java.Reflect;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectStruct {
    private final Class<?> cls;
    private final MethodAccess method;
    private final ConcurrentHashMap<String, AnnotationStruct[]> declaredMethodAnnotations;

    private ReflectStruct(Class<?> cls) {
        this.cls = cls;
        this.method = MethodAccess.get(cls);
        this.declaredMethodAnnotations = new ConcurrentHashMap<>();
        // 记录每个方法的注解
        this.loadMethodAnnotation(cls);
    }

    public static ReflectStruct build(Class<?> cls) {
        return new ReflectStruct(cls);
    }

    private void loadMethodAnnotation(Class<?> cls) {
        Method[] as = cls.getMethods();
        for (Method m : as) {
            Annotation[] ans = m.getDeclaredAnnotations();
            AnnotationStruct[] asa = new AnnotationStruct[ans.length];
            for (int i = 0; i < ans.length; i++) {
                asa[i] = AnnotationStruct.build(ans[i], m);
            }
            declaredMethodAnnotations.put(m.getName(), asa);
        }
    }

    public Class<?> getCls() {
        return cls;
    }

    public MethodAccess getMethod() {
        return method;
    }

    public AnnotationStruct[] getMethodAnnotation(String method_name) {
        return declaredMethodAnnotations.getOrDefault(method_name, null);
    }
}
