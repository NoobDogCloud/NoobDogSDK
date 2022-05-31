package common.java.Reflect;


import common.java.Apps.MicroService.MicroServiceContext;
import common.java.InterfaceModel.Type.InterfaceType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectStruct {
    private final Class<?> cls;
    private final MethodAccess method;
    private final ConcurrentHashMap<String, AnnotationStruct[]> declaredMethodAnnotations;

    private final String magicClassName;

    private ReflectStruct(Class<?> cls, String magicClassName) {
        this.cls = cls;
        this.method = MethodAccess.get(cls);
        this.declaredMethodAnnotations = new ConcurrentHashMap<>();
        this.magicClassName = magicClassName;
        // 记录每个方法的注解
        this.loadMethodAnnotation(cls);
    }

    public static ReflectStruct build(Class<?> cls) {
        return new ReflectStruct(cls, null);
    }

    public static ReflectStruct build(Class<?> cls, String magicClassName) {
        return new ReflectStruct(cls, magicClassName);
    }

    private Object buildAnnotation(int v) {
        return switch (v) {
            case 1 -> InterfaceType.type.SessionApi;
            case 2 -> InterfaceType.type.OauthApi;
            case 3 -> InterfaceType.type.PrivateApi;
            case 4 -> InterfaceType.type.CloseApi;
            default -> InterfaceType.type.PublicApi;
        };
    }

    private void loadMethodAnnotation(Class<?> cls) {
        Method[] as = cls.getMethods();
        for (Method m : as) {
            Annotation[] ans = m.getDeclaredAnnotations();
            List<AnnotationStruct> asa = new ArrayList<>();
            // magicClassName 不为空,根据模型接口定义生成注解
            if (ans.length == 0) {
                // 检查模型里定义
                if (magicClassName != null) {
                    var msc = MicroServiceContext.current();
                    if (msc != null) {
                        var model = msc.model(magicClassName);
                        if (model != null) {
                            var mapi = model.apiPerms();
                            if (mapi != null) {
                                // 获得 api 接口名
                                var apiPermValueArr = mapi.getPerm(m.getName() + "@" + _reflect.ParameterMethod(m));
                                if (apiPermValueArr != null) {
                                    for (var v : apiPermValueArr) {
                                        asa.add(AnnotationStruct.build(buildAnnotation(v)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 正常类
            for (Annotation an : ans) {
                asa.add(AnnotationStruct.build(an, m));
            }
            if (asa.size() > 0) {
                AnnotationStruct[] ansa = new AnnotationStruct[asa.size()];
                asa.toArray(ansa);
                declaredMethodAnnotations.put(m.getName(), ansa);
            }
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

    public String getMagicClassName() {
        return magicClassName;
    }
}
