package common.java.ServiceTemplate;

import java.util.HashSet;
import java.util.Set;

public class ServiceApiClass {
    private static final Set<String> updateApi = new HashSet<>();
    private static final Set<String> pullApi = new HashSet<>();

    static {
        updateApi.add("update");
        updateApi.add("updateEx");
        updateApi.add("insert");
        updateApi.add("delete");
        updateApi.add("deleteEx");
    }

    static {
        pullApi.add("select");
        pullApi.add("selectEx");
        pullApi.add("page");
        pullApi.add("pageEx");
    }

    public static boolean isUpdateAction(String methodName) {
        return updateApi.contains(methodName);
    }

    public static boolean isPullAction(String methodName) {
        return pullApi.contains(methodName);
    }

}
