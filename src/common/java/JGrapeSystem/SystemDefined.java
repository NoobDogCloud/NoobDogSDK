package common.java.JGrapeSystem;

import java.net.InetAddress;

public class SystemDefined {
    public static String ip() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
        }
        return null;
    }

    public static class interfaceSystemErrorCode {
        public final static int PublicApi = 10000;
        public final static int SessionApi = 10001;
        public final static int OauthApi = 10002;
        public final static int PrivateApi = 10003;
        public final static int CloseApi = 10004;
        public final static int VerifyApi = 10005;
    }

}
