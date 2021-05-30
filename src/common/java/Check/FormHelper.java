/*
 * 字段检查对象
 * */
package common.java.Check;

import common.java.Apps.MicroService.Model.MModelRuleNode;
import common.java.Number.NumberHelper;
import common.java.Session.UserSession;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import org.json.gsc.JSONObject;

import java.util.*;

public class FormHelper {
    private final HashMap<String, MModelRuleNode> checkObject;
    private final Set<String> maskCache;
    private final boolean filterMask = false;
    private JSONObject store;
    private boolean filterProtected = false;
    private boolean filterLocked = false;

    private boolean checkState;
    private String lastErrorKey;

    private FormHelper() {
        lastErrorKey = null;
        checkState = true;
        checkObject = new HashMap<>();
        maskCache = new HashSet<>();
        store = new JSONObject();
    }

    public static FormHelper build() {
        return new FormHelper();
    }

    public FormHelper store(JSONObject store) {
        this.store = store;
        return this;
    }

    //返回false不检查，直接算过
    private boolean checkAuth() {
        return checkState;
    }

    public FormHelper stopCheck() {
        checkState = false;
        return this;
    }

    public FormHelper resumeCheck() {
        checkState = true;
        return this;
    }

    //添加字段对象
    public FormHelper addField(MModelRuleNode field) {
        checkObject.put(field.name(), field);
        maskCache.clear();
        return this;
    }

    private void reInit() {
        filterProtected = false;
        filterLocked = false;
    }

    //过滤保护字段，从输入字段中删除保护属性的字段
    public FormHelper filterProtect() {
        filterProtected = true;
        return this;
    }

    //过滤锁定字段，从输入字段中删除锁定属性的字段
    public FormHelper filterLocked() {
        filterLocked = true;
        return this;
    }

    public JSONObject toJson() {
        try {
            if (!checkAuth()) {
                return store;
            }
            if (JSONObject.isInvalided(store)) {
                return store;
            }
            JSONObject rjson = new JSONObject();
            MModelRuleNode tf;
            for (String key : store.keySet()) {
                tf = checkObject.get(key);
                if (tf == null) {
                    continue;
                }
                if (filterProtected && tf.type() == MModelRuleNode.FieldType.protectField) {
                    continue;
                }
                if (filterLocked && tf.type() == MModelRuleNode.FieldType.lockerField) {
                    continue;
                }
                rjson.put(key, store.get(key));
            }
            return rjson;
        } finally {
            reInit();
        }
    }

    public String[] filterMask(String[] fields) {
        if (checkAuth()) {
            MModelRuleNode tf;
            List<String> newfield = new ArrayList<>();
            for (String field : fields) {
                if (checkObject.containsKey(field)) {
                    tf = checkObject.get(field);
                    if (tf.type() != MModelRuleNode.FieldType.maskField) {
                        newfield.add(field);
                    }
                }
            }
            String[] rs = new String[newfield.size()];
            newfield.toArray(rs);
            return rs;
        } else {
            return fields;
        }
    }

    //删除字段对象
    public FormHelper removeField(String fieldName) {
        if (checkObject.containsKey(fieldName)) {
            checkObject.remove(fieldName);
            maskCache.clear();
        }
        return this;
    }

    public FormHelper importField(HashMap<String, MModelRuleNode> newObj) {
        checkObject.putAll(newObj);
        maskCache.clear();
        return this;
    }

    //检查各个字段是否符合要求,也可以同时开启严格模式，要求字段必须符合要求
    public boolean checkTable(JSONObject inputData, boolean strict) {
        return _checkTable(inputData, false, strict);
    }

    //自己补齐字段
    public String autoComplete(String inputData) {
        return autoComplete(JSONObject.toJSON(inputData)).toString();
    }

    /**
     * 根据gsc-model( 包含 db-model )自动补充入库前的数据
     */
    public JSONObject autoComplete(JSONObject inputData) {
        // 填充db-model定义
        HashMap<String, MModelRuleNode> waitCheck = new HashMap<>(checkObject);
        // 过滤未定义字段
        JSONObject resultJson = new JSONObject();
        for (String key : waitCheck.keySet()) {
            resultJson.put(key, inputData.containsKey(key) ?
                    inputData.get(key) :
                    autoValueFilter(waitCheck.get(key).init())
            );
        }
        return resultJson;
    }

    /**
     * 判断当前值是否是特殊定义的值,如果是自动填充值
     * :timestamp	=>	当前时间戳
     */
    private Object autoValueFilter(Object nowValue) {
        Object rs = nowValue;
        if (nowValue instanceof String) {
            switch (nowValue.toString()) {
                case ":timestamp":
                    rs = TimeHelper.build().nowDatetime();
                    break;
                case ":user": {
                    UserSession se = UserSession.current();
                    if (se.checkSession()) {
                        rs = UserSession.current().getUID();
                    }
                    break;
                }
                case ":group": {
                    UserSession se = UserSession.current();
                    if (se.checkSession()) {
                        rs = UserSession.current().getGID();
                    }
                    break;
                }
            }
        }
        return rs;
    }

    /**
     * @param inputData 输入的数据
     * @param filter    是否开启过滤模式
     */
    private boolean _checkTable(JSONObject inputData, boolean filter, boolean strict) {
        MModelRuleNode tField;
        boolean rs = true;
        // JSONObject resultData = new JSONObject();
        if (checkAuth()) {
            for (String key : checkObject.keySet()) {
                tField = checkObject.get(key);
                if (inputData.containsKey(key)) {
                    CheckType checkType = tField.checkId();
                    Object val = inputData.get(key);
                    rs = checkType.forEachOr(in -> {
                        for (int j : in) {
                            if (!this.check(j, val)) {
                                return false;
                            }
                        }
                        // inputData.put(key, val);
                        return true;
                    });
                    if (!rs) {
                        if (filter) {
                            inputData.put(key, tField.failed());
                        }
                    }
                    // 有模型定义了，但是输入字段不包含的字段，自动按默认值填充，但是状态为错误
                } else {
                    if (filter) {
                        inputData.put(key, tField.init());
                    }
                    // 输入字段不包含定义字段，非严格模式下不算错
                    if (strict) {
                        rs = false;
                    }
                }
                if (filter) {
                    continue;
                }
                if (!rs) {
                    lastErrorKey = key;
                    break;
                }
            }
        }
        return rs;
    }

    public String getLastErrorName() {
        return lastErrorKey;
    }

    //检查数据，不符合的使用默认值填充
    public boolean filterAndCheckTable(JSONObject inputData, boolean strict) {
        return _checkTable(inputData, true, strict);
    }

    //返回字段信息
    public String[] getMaskFields() {
        if (maskCache.isEmpty()) {
            MModelRuleNode tf;
            for (String key : checkObject.keySet()) {
                tf = checkObject.get(key);
                if (tf != null) {
                    if (tf.type() == MModelRuleNode.FieldType.maskField) {
                        maskCache.add(key);
                    }
                }
            }
        }
        String[] nStringArray = new String[]{};
        if (maskCache.size() > 0) {
            nStringArray = new String[maskCache.size()];
            maskCache.toArray(nStringArray);
        }
        return nStringArray;
    }

    /**
     * @param ruleID 规则混合值
     * @return 返回去掉长度效验后的真实效验值, 失败则返回-1
     */
    private int checkLength(int ruleID, Object val) {
        if (ruleID < formdef.mixSpilt) {
            return ruleID;
        }
        String ruleString = String.valueOf(ruleID);
        int _ruleID = NumberHelper.number2int(StringHelper.build(ruleString).right(2).toString());
        int _maxLen = NumberHelper.number2int(StringHelper.build(ruleString).left(ruleString.length() - 2));
        return val.toString().length() <= _maxLen ? _ruleID : -1;
    }

    /**
     * 实际检查规则和数据
     * 通过为真，不通过为假
     */
    private boolean check(int rule, Object val) {
        String str = StringHelper.toString(val);
        if (str == null) {
            str = "";
        }
        int ruleID = checkLength(rule, str);
        switch (ruleID) {
            case formdef.notNull:
                return !CheckHelper.IsNull(str);
            case formdef.eqNull:
                return CheckHelper.IsNull(str);
            case formdef.bigZero:
                return CheckHelper.NotZero(str);
            case formdef.smallZero:
                return !CheckHelper.NotZero(str) && !CheckHelper.NotZero(str);
            case formdef.eqZero:
                return CheckHelper.IsZero(str);
            case formdef.number:
                return CheckHelper.IsInt(str);
            case formdef.Int:
                return CheckHelper.IsNum(str);
            case formdef.money:
                return CheckHelper.IsDecimal(str);
            case formdef.decimal:
                return CheckHelper.IsNum(str) && !CheckHelper.IsInt(str);
            case formdef.email:
                return CheckHelper.IsEmail(str);
            case formdef.mobile:
                return CheckHelper.IsMobileNumber(str);
            case formdef.BusinessID:
                return CheckHelper.IsBusinessRegisterNo(str);
            case formdef.Chinese:
                return CheckHelper.IsChinese(str);
            case formdef.id:
                return CheckHelper.IsID(str, 64);
            case formdef.noSpace:
                return CheckHelper.notContainSpace(str);
            case formdef.name:
                return CheckHelper.IsRealName(str);
            case formdef.PersonID:
                return CheckHelper.IsPersonCardID(str);
            case formdef.simpleDate:
                return CheckHelper.IsDate(str);
            case formdef.week:
                return CheckHelper.IsWeek(str);
            case formdef.month:
                return CheckHelper.IsMonth(str);
            case formdef.ip:
                return CheckHelper.IsIP(str);
            case formdef.url:
                return CheckHelper.IsUrl(str);
            case formdef.password:
                return CheckHelper.IsPassword(str);
            case formdef.postCode:
                return CheckHelper.IsPostalCode(str);
            case formdef.Date:
                return CheckHelper.IsDateAndYear(str);
            case formdef.Time:
                return CheckHelper.IsTime(str);
            case formdef.UnixDate:
                try {
                    long l = Long.parseLong(str);
                    return CheckHelper.IsUnixDate(l);
                } catch (Exception e) {
                }
                break;
            case formdef.BankCard:
                return CheckHelper.IsBankCard(str);
            case formdef.ObjectId:
                return CheckHelper.IsObject(str);
            default:
                // rule不在有效范围,效验失败
                return false;
        }
        return false;
    }

    public static class formdef {
        /**
         * 复合规则分界线
         * <p>
         * |--|---
         * /效验规则/最大字符长度
         */
        public final static int mixSpilt = 1000;
        /**
         * 不为空
         */
        public final static int notNull = 1;
        /**
         * 为空
         */
        public final static int eqNull = 2;
        /**
         * 大于0
         */
        public final static int bigZero = 3;
        /**
         * 小于0
         */
        public final static int smallZero = 4;
        /**
         * 等于0
         */
        public final static int eqZero = 5;
        /**
         * 整数
         */
        public final static int number = 6;
        /**
         * 自然数，包含小数
         */
        public final static int Int = 7;
        /**
         * 小数点后有2位
         */
        public final static int money = 8;
        /**
         * 小数
         */
        public final static int decimal = 9;
        /**
         * email
         */
        public final static int email = 10;
        /**
         * 手机号
         */
        public final static int mobile = 11;
        /**
         * 工商执照号
         */
        public final static int BusinessID = 12;
        /**
         * 纯中文
         */
        public final static int Chinese = 13;
        /**
         * id
         */
        public final static int id = 26;
        /**
         * 不包含空格
         */
        public final static int noSpace = 14;
        /**
         * 真实姓名
         */
        public final static int name = 15;
        /**
         * 身份证号
         */
        public final static int PersonID = 16;
        /**
         * 常见的有效时间验证，年月日，时分秒
         */
        public final static int simpleDate = 17;
        /**
         * 星期
         */
        public final static int week = 18;
        /**
         * 月
         */
        public final static int month = 19;
        /**
         * ip
         */
        public final static int ip = 20;
        /**
         * url
         */
        public final static int url = 21;
        /**
         * 密码
         */
        public final static int password = 22;
        /**
         * 中国邮政编码
         */
        public final static int postCode = 23;
        /**
         * 支持闰年的年月日，时秒分
         */
        public final static int Date = 24;
        /**
         * 时间
         */
        public final static int Time = 25;
        /**
         * unix时间戳
         */
        public final static int UnixDate = 27;
        /**
         * 银行卡号
         */
        public final static int BankCard = 28;
        /**
         * objectId
         */
        public final static int ObjectId = 29;
    }
}
