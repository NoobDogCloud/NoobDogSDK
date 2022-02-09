package common.java.Check;

import common.java.String.StringHelper;
import common.java.Time.TimeHelper;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckHelper {
    /**
     * 是否为空
     *
     * @param
     * @return
     */
    public static boolean IsNull(String str) {
        return str == null || str.isEmpty();
    }


    /**
     * 不是为0的数字
     *
     * @param str
     * @return
     */
    public static boolean IsZero(String str) {
        if (IntTest(str)) {
            return Integer.parseInt(str) == 0;
        }
        if (LongTest(str)) {
            return Long.parseLong(str) == 0;
        }
        if (FloatTest(str)) {
            return Float.parseFloat(str) == (float) 0.0;
        }
        if (DoubleTest(str)) {
            return Double.parseDouble(str) == 0.00;
        }
        return false;
    }

    public static boolean NotZero(String str) {
        if (IntTest(str)) {
            return Integer.parseInt(str) > 0;
        }
        if (LongTest(str)) {
            return Long.parseLong(str) > 0;
        }
        if (FloatTest(str)) {
            return Float.parseFloat(str) > (float) 0.0;
        }
        if (DoubleTest(str)) {
            return Double.parseDouble(str) > 0.00;
        }
        return false;
    }

    /**
     * 是否是整数
     *
     * @param str
     * @return
     */
    public static boolean IsInt(String str) {
        return IntTest(str) || LongTest(str);
    }

    private static boolean IntTest(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean LongTest(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean FloatTest(String str) {
        try {
            Float.parseFloat(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean DoubleTest(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 是否是数字
     *
     * @param str
     * @return
     */
    public static boolean IsNum(String str) {
        return FloatTest(str) || DoubleTest(str);
    }

    /**
     * 验证邮箱
     *
     * @param email
     * @return
     */
    public static boolean IsEmail(String email) {
        String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        return match(check, email);
    }

    /**
     * 验证手机号码
     *
     * @param
     * @return
     */
    public static boolean IsMobileNumber(String mobileNumber) {
        String check = "^(((13[0-9])|(15([0-3]|[5-9]))|(17([0-9]))|(18[0-9]))\\d{8})|(0\\d{2}-\\d{8})|(0\\d{3}-\\d{7})$";
        return match(check, mobileNumber) && mobileNumber.length() == 11;
    }

    /**
     * 验证固定电话号码
     *
     * @param
     * @return
     */
    public static boolean IsTelPhoneNumber(String telphoneNumber) {
        String check1 = "^(([0\\+]\\d{2,3}-)?(0\\d{2,3})-)(\\d{7,8})(-(\\d{3,}))?$";
        String check2 = "^(\\d{7,8})(-(\\d{3,}))?$";
        return (match(check1, telphoneNumber) && telphoneNumber.length() == 12) || (match(check2, telphoneNumber) && telphoneNumber.length() == 7);
    }

    /**
     * 验证工商执照
     *
     * @param str
     * @return
     */
    public static boolean IsBusinessRegisterNo(String str) {
        String check = "^[0-9][a-fA-F0-9]{14,18}$";
        return match(check, str) && (str.length() == 15 || str.length() == 18);
    }

    /**
     * 验证是否是中文
     *
     * @param str
     * @return
     */
    public static boolean IsChinese(String str) {
        String check = "^[\\x{4e00}-\\x{9fa5}]+$";
        return match(check, str);
    }

    /**
     * 验证ID
     *
     * @param str
     * @param len
     * @return
     */
    public static boolean IsID(String str, int len) {
        String check = "^[a-zA-Z][a-z0-9A-Z_-]{2," + len + "}+$";
        return match(check, str);
    }

    public static boolean IsStrictID(String str, int len) {
        String check = "^[a-zA-Z][a-z0-9A-Z-]{2," + len + "}+$";
        return match(check, str);
    }

    public static boolean notContainSpace(String str) {
        return !(str.contains(" "));
    }

    /**
     * 验证真实姓名
     *
     * @param str
     * @return
     */
    public static boolean IsRealName(String str) {
        int l = str.length();
        return IsChinese(str) ? (l > 1 && l < 5 && !notContainSpace(str)) : (l > 2 && l < 255);
    }

    /**
     * 验证身份证号合法性
     *
     * @param str
     * @return
     */
    public static boolean IsPersonCardID(String str) {
        return PersonCardID.isValidatedAllIdcard(str);
    }

    /**
     * 验证数字是否是有效时间
     *
     * @param unixtime
     * @return
     */
    public static boolean IsUnixDate(long unixtime) {
        return unixtime == 0 || TimeHelper.build().timestampToDate(unixtime) != null;
    }

    /**
     * 验证是否是有效时间,字符串模式
     *
     * @param str
     * @return
     */
    public static boolean IsDate(String str) {
        boolean flag = false;
        List<String> timeFormatYMD = new ArrayList<>();
        List<String> timeFormatHMS = new ArrayList<>();
        timeFormatYMD.add("yyyy-MM-dd");
        timeFormatYMD.add("yyyy/MM/dd");
        timeFormatYMD.add("yyyy年MM月dd日");
        timeFormatYMD.add(null);

        timeFormatHMS.add("HH:mm:ss");
        timeFormatHMS.add("HH:mm");
        timeFormatHMS.add("HH点mm分ss秒");
        timeFormatHMS.add("HH点mm分");
        timeFormatHMS.add("HH时mm分ss秒");
        timeFormatHMS.add("HH时mm分");
        timeFormatHMS.add(null);
        String _format;
        for (String _formatYMD : timeFormatYMD) {
            for (String _formatHMS : timeFormatHMS) {
                try {
                    _format = _formatYMD == null ? _formatHMS : _formatYMD + " " + _formatHMS;
                    if (_formatYMD == null && _formatHMS == null) {
                        break;
                    }
                    // nLogger.logInfo(_format);
                    DateTimeFormatter format = DateTimeFormatter.ofPattern(_format);
                    LocalDateTime.parse(str, format);
                    return true;
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    /**
     * 是否是星期
     *
     * @param str
     * @return
     */
    public static boolean IsWeek(String str) {
        String tmp;
        String _char;
        boolean state = true;
        while (state) {
            if (str.length() <= 1) {
                break;
            }
            _char = StringHelper.build(str).charAtFirst().toString();
            switch (_char) {
                case "星", "期", "礼", "拜", "周" -> {
                    str = StringHelper.build(str).removeTrailingFrom().toString();
                    state = true;
                    break;
                }
                default -> state = false;
            }
        }
        tmp = switch (str) {
            case "一" -> "1";
            case "二" -> "2";
            case "三" -> "3";
            case "四" -> "4";
            case "五" -> "5";
            case "六" -> "6";
            case "日", "天", "七" -> "7";
            default -> str;
        };
        return IsInt(tmp) && (Integer.parseInt(tmp) > 0 && Integer.parseInt(tmp) < 8);
    }

    /**
     * 是否是月
     *
     * @param str
     * @return
     */
    public static boolean IsMonth(String str) {
        String tmp;
        String _char;
        boolean state = true;
        while (state) {
            if (str.length() <= 1) {
                break;
            }
            _char = StringHelper.build(str).charAtLast().toString();
            switch (_char) {
                case "月", "份" -> {
                    str = StringHelper.build(str).removeTrailingFrom().toString();
                    state = true;
                    break;
                }
                default -> state = false;
            }
        }

        tmp = switch (str) {
            case "一" -> "1";
            case "二" -> "2";
            case "三" -> "3";
            case "四" -> "4";
            case "五" -> "5";
            case "六" -> "6";
            case "七" -> "7";
            case "八" -> "8";
            case "九" -> "9";
            case "十" -> "10";
            case "十一" -> "11";
            case "十二" -> "12";
            default -> str;
        };
        return IsInt(tmp) && (Integer.parseInt(tmp) > 0 && Integer.parseInt(tmp) < 13);
    }

    /**
     * 判断是否是IP
     *
     * @param str
     * @return
     */
    public static boolean IsIP(String str) {
        String num = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";
        String regex = "^" + num + "\\." + num + "\\." + num + "\\." + num + "$";
        return match(regex, str);
    }

    /**
     * 判断是否是URL网址
     *
     * @param str
     * @return
     */
    public static boolean IsUrl(String str) {
        boolean rs;
        try {
            new URL(str);
            rs = true;
        } catch (Exception e) {
            rs = false;
        }
        return rs;
    }

    /**
     * 密码验证
     * 包含数字和字母的6-20位字符
     *
     * @param str
     * @return
     */
    public static boolean IsPassword(String str) {
        String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,128}$";
        return match(regex, str);
    }

    /**
     * 中国邮政编码验证
     *
     * @param str
     * @return
     */
    public static boolean IsPostalCode(String str) {
        String regex = "^\\d{6}$";
        return match(regex, str);
    }

    /**
     * 小数点后2位数字(金额)
     *
     * @param str
     * @return
     */
    public static boolean IsDecimal(String str) {
        String regex = "^(([1-9]+)|([0-9]+\\.[0-9]{1,2}))$";
        return match(regex, str);
    }

    /**
     * 支持闰年的时间和日期
     *
     * @param str
     * @return
     */
    public static boolean IsDateAndYear(String str) {
        //String regex = "^\\d{4}-(?:0\\d|1[0-2])-(?:[0-2]\\d|3[01])( (?:[01]\\d|2[0-3])\\:[0-5]\\d\\:[0-5]\\d)?$";
        String regex = "(((01[0-9]{2}|0[2-9][0-9]{2}|[1-9][0-9]{3})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|((01[0-9]{2}|0[2-9][0-9]{2}|[1-9][0-9]{3})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|((01[0-9]{2}|0[2-9][0-9]{2}|[1-9][0-9]{3})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((04|08|12|16|[2468][048]|[3579][26])00))-0?2-29)) (20|21|22|23|[0-1]?\\d):[0-5]?\\d:[0-5]?\\d";
        return match(regex, str);
    }

    /**
     * 时间验证
     *
     * @param str
     * @return
     */
    public static boolean IsTime(String str) {
        String regex = "^(?:[01]\\d|2[0-3])(?::[0-5]\\d){1,2}$";
        return match(regex, str);
    }

    /**
     * 银行卡号验证
     *
     * @param str
     * @return
     */
    public static boolean IsBankCard(String str) {
        return BankCard.checkBankCard(str);
    }

    public static boolean IsObject(String str) {
        String regex = "^[0-9a-fA-F]{24}$";
        return match(regex, str);
    }

    public static boolean IsVersion(String str) {
        String regex = "^(([0-9]|([1-9]([0-9]*))).){2}([0-9]|([1-9]([0-9]*)))([-](([0-9A-Za-z]|([1-9A-Za-z]([0-9A-Za-z]*)))[.]){0,}([0-9A-Za-z]|([1-9A-Za-z]([0-9A-Za-z]*)))){0,1}([+](([0-9A-Za-z]{1,})[.]){0,}([0-9A-Za-z]{1,})){0,1}$";
        return match(regex, str);
    }

    private static boolean match(String regex, String str) {
        boolean flag;
        try {
            Pattern reg = Pattern.compile(regex);
            Matcher matcher = reg.matcher(str);
            flag = matcher.matches();
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }
}