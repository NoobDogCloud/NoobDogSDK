package common.java.Check;

import java.util.function.Function;

public class CheckType {
    private final String[] orArray;

    public CheckType(String typeString) {
        orArray = typeString.split("\\|");
    }

    public boolean forEachOr(Function<int[], Boolean> func) {
        for (String s : orArray) {
            String[] andArray = s.split("&");
            int[] andIntArray = new int[andArray.length];
            for (int n = 0, m = andArray.length; n < m; n++) {
                andIntArray[n] = Integer.parseInt(andArray[n]);
            }
            // 任意一个and组满足条件，验证正确
            if (func.apply(andIntArray)) {
                return true;
            }
        }
        return false;
    }
}
