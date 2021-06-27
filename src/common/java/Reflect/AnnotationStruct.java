package common.java.Reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationStruct {
    private final Class<? extends Annotation> type;
    private final Object val;

    private AnnotationStruct(Annotation an, Method m) {
        type = an.annotationType();
        val = m.getAnnotation(type);
    }

    public static AnnotationStruct build(Annotation an, Method m) {
        return new AnnotationStruct(an, m);
    }

    public Class<?> getType() {
        return type;
    }

    public <T> T getVal() {
        return (T) val;
    }
}
