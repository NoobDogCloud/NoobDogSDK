package common.java.Rpc;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.Model.MicroModel;
import common.java.Encrypt.GscJson;
import common.java.HttpServer.Common.RequestSession;
import common.java.HttpServer.HttpContext;
import common.java.Reflect._reflect;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ExecRequest {//框架内请求类

    private static final HashMap<Class<?>, String> class2string;
    private static final String appsURL;
    private static final ConcurrentHashMap<String, List<Object>> BeforeFilterObjectCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Object>> AfterFilterObjectCache = new ConcurrentHashMap<>();

    static {

        class2string = new HashMap<>();
        class2string.put(String.class, "s");
        class2string.put(int.class, "i,int");
        class2string.put(long.class, "l,long");
        class2string.put(char.class, "char");
        class2string.put(float.class, "f,float");
        class2string.put(boolean.class, "b,boolean");
        class2string.put(short.class, "short");
        class2string.put(double.class, "d,double");

        class2string.put(Integer.class, "i,int");
        class2string.put(Long.class, "l,long");
        class2string.put(Character.class, "char");
        class2string.put(Float.class, "f,float");
        class2string.put(Boolean.class, "b,boolean");
        class2string.put(Short.class, "short");
        class2string.put(Double.class, "d,double");

        class2string.put(JSONObject.class, "j,json");
        class2string.put(JSONArray.class, "ja,jsonArray");
        appsURL = null;
    }

    private static Object ModelDesc(HttpContext ctx) {
        AppContext aCtx = AppContext.current();
        if (aCtx == null) {
            return RpcError.Instant(false, "无效应用");
        }
        var mServInfo = aCtx.microServiceInfo();
        if (mServInfo == null) {
            return RpcError.Instant(false, "无效服务");
        }
        JSONObject r = new JSONObject();
        HashMap<String, MicroModel> h = mServInfo.model();
        for (String key : h.keySet()) {
            // 仅获得模型定义
            r.put(key, h.get(key).ruleArray().toJsonArray());
        }
        return r;
    }

    /**
     * 全局服务
     */
    private static Object global_class_service(HttpContext ctx) {
        Object rs = null;
        try {
            if ("@getModel".equalsIgnoreCase(ctx.className())) {
                rs = ModelDesc(ctx);
            }
        } catch (Exception e) {
            rs = "系统服务[" + ctx.className() + "]异常";
        }
        return rs;
    }

    /**
     * 执行当前上下文环境下的调用
     */
    public static Object _run(HttpContext hCtx) {
        // HttpContext hCtx = HttpContext.current();
        Object rs = global_class_service(hCtx);
        if (rs == null) {
            String className = hCtx.className();
            String actionName = hCtx.actionName();
            try {
                // 执行转换前置类
                Object[] _objs = convert2GscCode(hCtx.invokeParamter());
                FilterReturn filterReturn = beforeExecute(className, actionName, _objs);
                if (filterReturn.state()) {
                    // 载入主类
                    Class<?> _cls = Class.forName("main.java.Api._Api" + "." + className);
                    // 执行call
                    try {
                        // 创建类反射
                        _reflect obj = new _reflect(_cls);
                        // 保存反射类
                        RequestSession.setCurrent(obj);
                        // 构造反射类实例
                        obj.newInstance();
                        // 调用主要类,后置类,固定返回结构
                        rs = obj._call(actionName, _objs);
                        rs = RpcResult(afterExecute(className, actionName, _objs, rs));
                    } catch (Exception e) {
                        nLogger.logInfo(e, "实例化 " + _cls + " ...失败");
                    }
                } else {
                    rs = RpcError.Instant(filterReturn);
                }
            } catch (Exception e) {
                nLogger.logInfo(e, "类:" + className + " : 不存在");
            }
        }
        return rs;
    }

    private static List<Object> getFilterCache(String classFullName, ConcurrentHashMap<String, List<Object>> cacheObject, Class<?>... pCls) {
        List<Object> o_array;
        try {
            o_array = cacheObject.get(classFullName);
            if (o_array == null) {
                Class<?> _after_cls = Class.forName(classFullName);
                Constructor<?> cObject = _after_cls.getDeclaredConstructor(null);
                try {
                    // String.class, String.class, ReturnCallback.class
                    Method f = _after_cls.getMethod("filter", pCls);
                    o_array = new ArrayList<>();
                    o_array.add(cObject.newInstance(null));
                    o_array.add(f);
                    cacheObject.put(classFullName, o_array);
                } catch (Exception e) {
                    return null;
                }
            }
        } catch (Exception e) {
            o_array = null;
        }
        return o_array;
    }

    // 转换 GscJson 参数(请求层转换)
    private static Object[] convert2GscCode(Object[] objs) {
        if (objs == null) {
            return null;
        }
        for (int i = 0; i < objs.length; i++) {
            Object o = objs[i];
            if (o instanceof String) {
                String v = (String) o;
                var header = GscJson.getHeader(v);
                if (header != null) {
                    switch (GscJson.getType(header)) {
                        case "json":
                            objs[i] = GscJson.decodeJson(v);
                            break;
                        case "jsonArray":
                            objs[i] = GscJson.decodeJsonArray(v);
                            break;
                    }
                }
            }
        }
        return objs;
    }

    // 过滤函数改变输入参数
    private static FilterReturn beforeExecute(String className, String actionName, Object[] objs) {
        String classFullName = "main.java.Api._Before" + "." + className;
        List<Object> o_array = getFilterCache(classFullName, BeforeFilterObjectCache, String.class, String.class, Object[].class);
        if (o_array == null) {  // 没有过滤函数
            return FilterReturn.buildTrue();
        }
        Object o = o_array.get(0);
        Method f = (Method) o_array.get(1);
        try {
            return (FilterReturn) f.invoke(o, className, actionName, objs);
        } catch (Exception e) {
            return FilterReturn.build(false, "过滤函数异常");
        }
    }

    // 结果函数改变输入参数
    private static Object afterExecute(String className, String actionName, Object[] parameter, Object obj) {
        String classFullName = "main.java.Api._After" + "." + className;
        List<Object> o_array = getFilterCache(classFullName, AfterFilterObjectCache, String.class, String.class, Object[].class, Object.class);
        if (o_array == null) {  // 没有过滤函数
            return obj;
        }
        Object o = o_array.get(0);
        Method f = (Method) o_array.get(1);
        try {
            return f.invoke(o, className, actionName, parameter, obj);
        } catch (Exception e) {
            return obj;
        }
    }

    private static Object RpcResult(Object o) {
        if (o == null) {
            return rMsg.netState(false);
        }
        if (o instanceof String ||
                o instanceof Integer ||
                o instanceof List<?> ||
                o instanceof HashMap<?, ?> ||
                o instanceof Long ||
                o instanceof Float ||
                o instanceof Double ||
                o instanceof Short ||
                o instanceof Boolean ||
                o instanceof JSONObject ||
                o instanceof JSONArray
        ) {
            return rMsg.netMSG(o);
        }
        return o;
    }

    /**
     * java类型转成字符串类型
     */
    public static String class2string(Class<?> cls) {
        return class2string.containsKey(cls) ? class2string.get(cls).split(",")[0] : cls.getSimpleName();
    }

    private static boolean is_grape_args(String arg) {
        return arg != null && arg.split(":").length > 1;
    }

    private static String rpc_parameter2string(Object obj) {
        if (obj instanceof JSONObject) {
            return GscJson.encodeJson((JSONObject) obj);
        } else if (obj instanceof JSONArray) {
            return GscJson.encodeJsonArray((JSONArray) obj);
        } else {
            return StringHelper.toString(obj);
        }
    }

    public static String objects2string(Object[] objs) {
        if (objs == null) {
            return "";
        }
        String value;
        StringBuilder rString = new StringBuilder();
        for (Object val : objs) {
            rString.append("/");
            if (!is_grape_args(val.toString())) {
                value = class2string(val.getClass());
                rString.append(value).append(":");
            }
            rString.append(rpc_parameter2string(val));
        }
        return rString.toString();
    }

    public static String objects2poststring(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        String[] GetParams = StringHelper.build(ExecRequest.objects2string(args)).trimFrom('/').toString().split("/");
        return "gsc-post:" + StringHelper.join(GetParams, ":,");
    }

    public static Object[] postJson2ObjectArray(JSONObject postParameter) {
        Object[] args = null;
        if (postParameter != null) {
            int i = 0;
            args = new Object[postParameter.size()];
            for (String key : postParameter.keySet()) {
                args[i] = postParameter.get(key);
            }
        }
        return args;
    }

    public static String objects2poststring(JSONObject info) {
        if (info == null || info.size() == 0) {
            return "";
        }
        // String[] GetParams = StringHelper.build(ExecRequest.objects2string(args)).trimFrom('/').toString().split("/");
        StringBuilder GetParams = new StringBuilder();
        for (String key : info.keySet()) {
            GetParams.append(info.getString(key)).append(":;");
        }
        return "gsc-post:" + StringHelper.build(GetParams.toString()).removeTrailingFrom(2).toString();
    }

}