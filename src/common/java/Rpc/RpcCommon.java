package common.java.Rpc;

public class RpcCommon {
    /**
     * java参数变gsc-rpc参数
     */
    public static String paramer2string(Object... args) {
        return ExecRequest.objects2string(args);
    }
}
