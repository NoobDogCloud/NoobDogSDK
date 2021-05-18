package common.java.CheckCode;


import common.java.Interrupt.CacheAuth;

import java.util.Random;

public class CheckCodeHelper {
    private static final String VERIFY_CODES = "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    public static String getCheckCode(String ssid, int len) {
        String randNo = generateVerifyCode(len);
        CacheAuth cacheAuth = new CacheAuth();
        cacheAuth.breakRun(ssid, randNo);
        return randNo;
    }

    public static byte[] getCheckImage(String ssid, int len) {
        return ImageCheckCode.getCodeimage(getCheckCode(ssid, len));
    }

    public static boolean checkCode(String ssid, String code) {
        CacheAuth cacheAuth = new CacheAuth();
        return cacheAuth.resumeRun(ssid, code);
    }

    /**
     * 使用系统默认字符源生成验证码
     *
     * @param verifySize 验证码长度
     * @return
     */
    public static String generateVerifyCode(int verifySize) {
        return generateVerifyCode(verifySize, VERIFY_CODES);
    }

    /**
     * 使用指定源生成验证码
     *
     * @param verifySize 验证码长度
     * @param sources    验证码字符源
     * @return
     */
    private static String generateVerifyCode(int verifySize, String sources) {
        if (sources == null || sources.length() == 0) {
            sources = VERIFY_CODES;
        }
        int codesLen = sources.length();
        Random rand = new Random(System.currentTimeMillis());
        StringBuilder verifyCode = new StringBuilder(verifySize);
        for (int i = 0; i < verifySize; i++) {
            verifyCode.append(sources.charAt(rand.nextInt(codesLen - 1)));
        }
        return verifyCode.toString();
    }
}
