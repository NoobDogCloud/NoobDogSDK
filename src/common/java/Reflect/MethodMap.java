package common.java.Reflect;

import java.lang.reflect.Method;

public record MethodMap(Class<?>[] paramClassArr, Method method) {
}
