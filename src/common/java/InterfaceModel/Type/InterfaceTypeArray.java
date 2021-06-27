package common.java.InterfaceModel.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceTypeArray {
    InterfaceType[] value();
}