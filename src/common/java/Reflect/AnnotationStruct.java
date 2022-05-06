package common.java.Reflect;

import common.java.InterfaceModel.Type.InterfaceType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationStruct {
    private final Class<? extends Annotation> type;
    private final Object val;

    private AnnotationStruct(Annotation an, Method m) {
        type = an.annotationType();
        val = m.getAnnotation(type);
    }

    private AnnotationStruct(Object val) {
        this.type = InterfaceType.class;
        this.val = val;
    }

    public static AnnotationStruct build(Annotation an, Method m) {
        return new AnnotationStruct(an, m);
    }

    public static AnnotationStruct build(Object val) {
        return new AnnotationStruct(val);
    }

    public Class<?> getType() {
        return type;
    }

    public <T> T getVal() {
        return (T) val;
    }
}
