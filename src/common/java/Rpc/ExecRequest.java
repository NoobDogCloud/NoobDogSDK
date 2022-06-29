package common.java.Rpc;

import common.java.Apps.AppContext;
import common.java.Apps.MicroService.MicroServiceContext;
import common.java.Apps.MicroService.Model.MicroModel;
import common.java.DataSource.CustomDataSourceSubscriber;
import common.java.Encrypt.GscEncrypt;
import common.java.Http.Server.ApiSubscribe.GscSubscribe;
import common.java.Http.Server.HttpContext;
import common.java.JGrapeSystem.GrapeJar;
import common.java.Reflect.ReflectStruct;
import common.java.Reflect._reflect;
import common.java.ServiceTemplate.MicroServiceTemplate;
import common.java.ServiceTemplate.ServiceApiClass;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class ExecRequest {//框架内请求类

    private static final HashMap<Class<?>, String> class2string;
    private static final ConcurrentHashMap<String, RpcFilterFnCache> BeforeFilterObjectCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RpcFilterFnCache> AfterFilterObjectCache = new ConcurrentHashMap<>();

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
        class2string.put(Object.class, "o,object");

    }

    private static Object ModelDesc(HttpContext ctx) {
        AppContext aCtx = AppContext.current();
        if (aCtx == null) {
            return RpcMessage.Instant(false, "无效应用");
        }
        var mServInfo = aCtx.microServiceInfo();
        if (mServInfo == null) {
            return RpcMessage.Instant(false, "无效服务");
        }
        JSONObject r = new JSONObject();
        HashMap<String, MicroModel> h = mServInfo.model();
        for (String key : h.keySet()) {
            // 仅获得模型定义
            r.put(key, h.get(key).ruleArray().toJsonArray());
        }
        return rMsg.netMSG(r);
    }

    private static boolean IsGlobalService(HttpContext ctx) {
        return ctx.serviceName().toLowerCase(Locale.ROOT).equals("global");
    }

    private static Object CustomDataSource(HttpContext ctx) {
        // 不是全局服务
        if (!IsGlobalService(ctx)) {
            return null;
        }
        var header = ctx.header();
        String mode = header.getString(HttpContext.GrapeHttpHeader.WebSocketHeader.wsMode);
        if (mode == null) {
            return null;
        }
        String topic = header.getString(HttpContext.GrapeHttpHeader.WebSocketHeader.wsTopic);
        if (topic == null) {
            return null;
        }
        switch (mode) {
            case "subscribe" -> {
                var cls = CustomDataSourceSubscriber.build(topic);
                return rMsg.netMSG(cls != null ? cls.result() : "");
            }
            case "select" -> {
                var cls = CustomDataSourceSubscriber.build(topic);
                return rMsg.netMSG(cls != null ? cls.getAllData() : "");
            }
            default -> CustomDataSourceSubscriber.cancel(ctx.channelContext());
        }
        return true;
    }

    /**
     * 全局服务
     */
    private static Object global_class_service(HttpContext ctx) {
        Object rs = null;
        try {
            switch (ctx.className()) {
                // 获得服务模型
                case "@getModel" -> rs = ModelDesc(ctx);
                // 获得订阅自定义源
                case "@subscribeCustomDataSource" -> rs = CustomDataSource(ctx);
                // 获得服务所有类和接口说明
                case "@description" -> rs = _reflect.ServDeclAll();
            }
        } catch (Exception e) {
            rs = RpcMessage.Instant(false, "系统服务[" + ctx.className() + "]异常");
        }
        return rs;
    }

    private static final ConcurrentHashMap<String, ReflectStruct> share_class = new ConcurrentHashMap<>();
    public static final String ExecBaseFolder = "main.java.Api.";


    /**
     * 遍历 api 目录下所有类
     */
    /**
     * public static void loadServiceApi() {
     * List<Class<?>> clsArr = GrapeJar.getClass(ExecBaseFolder + "._Api", true);
     * // 修改每个载入的 class,增加调用方法
     * for (Class<?> cls : clsArr) {
     * share_class.put(cls.getSimpleName(), ReflectStruct.build(cls));
     * }
     * }
     */
    // graalvm native编译时,不可以使用
    public static void preloadServiceClass() {
        String folderName = StringHelper.trimFrom(ExecBaseFolder, '.');
        List<Class<?>> clsArr = GrapeJar.getClass(folderName, true);
        for (Class<?> cls : clsArr) {
            String simpleName = cls.getSimpleName();
            if (simpleName.endsWith("After")) {
                var name = simpleName.substring(0, simpleName.length() - 5);
                if (!name.isEmpty()) {
                    getServiceAfter(name);
                }
            } else if (simpleName.endsWith("Before")) {
                var name = simpleName.substring(0, simpleName.length() - 6);
                if (!name.isEmpty()) {
                    getServiceBefore(name);
                }
            } else {
                ReflectStruct _cls = getServiceApi(simpleName);
                if (_cls != null) {
                    try (var h = _reflect.build(_cls)) {
                        // nothing
                        _reflect.getServDecl(_cls.getCls());
                    }
                }
            }
        }
    }

    private static ReflectStruct loadMagicServiceApiClass(String name) {
        try {
            return ReflectStruct.build(Class.forName(ExecBaseFolder + name));
        } catch (Exception e) {
            // 本地类不存在
            if (MicroServiceContext.current().model().containsKey(name)) {
                return ReflectStruct.build(MicroServiceTemplate.class, name);
            }
            return null;
        }
    }

    /**
     * 动态载入类
     *
     * @return
     */
    public static ReflectStruct getServiceApi(String name) {
        // 增加对未知类初始化支持(有业务模型的未知类,类名称与模型名称相同)
        if (!share_class.containsKey(name)) {
            var v = loadMagicServiceApiClass(name);
            if (v == null) {
                return null;
            }
            share_class.put(name, v);
        }
        return share_class.get(name);
    }

    /**
     * 遍历 api_before 目录下所有类
     */
    /**
     * public static void loadServiceBefore() {
     * List<Class<?>> clsArr = GrapeJar.getClass(ExecBaseFolder + "._Before", true);
     * // 修改每个载入的 class,增加调用方法
     * for (Class<?> cls : clsArr) {
     * BeforeFilterObjectCache.put(cls.getSimpleName(), RpcFilterFnCache.build(cls));
     * }
     * }
     */
    public static RpcFilterFnCache getServiceBefore(String name) {
        if (!BeforeFilterObjectCache.containsKey(name)) {
            Class<?> cls;
            try {
                cls = Class.forName(ExecBaseFolder + name + "Before");
            } catch (Exception e) {
                return null;
            }
            BeforeFilterObjectCache.put(name, RpcFilterFnCache.build(cls));
        }
        return BeforeFilterObjectCache.get(name);
    }

    /**
     * 遍历 api_before 目录下所有类
     */
    /**
     * public static void loadServiceAfter() {
     * List<Class<?>> clsArr = GrapeJar.getClass(ExecBaseFolder + "._After", true);
     * // 修改每个载入的 class,增加调用方法
     * for (Class<?> cls : clsArr) {
     * AfterFilterObjectCache.put(cls.getSimpleName(), RpcFilterFnCache.build(cls));
     * }
     * }
     */
    public static RpcFilterFnCache getServiceAfter(String name) {
        if (!AfterFilterObjectCache.containsKey(name)) {
            Class<?> cls;
            try {
                cls = Class.forName(ExecBaseFolder + name + "After");
            } catch (Exception e) {
                return null;
            }
            AfterFilterObjectCache.put(name, RpcFilterFnCache.build(cls));
        }
        return AfterFilterObjectCache.get(name);
    }

    private static String getServiceTopic(HttpContext hCtx) {
        String topic = hCtx.topic();
        if (!StringHelper.isInvalided(topic)) {
            return topic;
        }
        return "topic_service_" + hCtx.serviceName() + "_" + hCtx.className();
    }

    public static Object redirectRequest(HttpContext hCtx) {
        Object rs = null;
        // 判断是否包含代理服务,是则转发服务
        JSONArray<String> proxyArr = MicroServiceContext.current().getProxyService();
        if (proxyArr.size() == 0) {
            return RpcMessage.Instant(false, "请求错误 ->目标[" + hCtx.className() + "]不存在！");
        }
        for (String serviceName : proxyArr) {
            try {
                rs = rpc.context(hCtx).setService(serviceName).call(hCtx.invokeParamter());
            } catch (Exception e) {
                nLogger.warnInfo("服务->" + hCtx.serviceName() + " 调用代理服务:" + serviceName + "...失败!");
            }
        }
        return rs;
    }

    /**
     * 执行当前上下文环境下的调用
     */
    public static Object _run(HttpContext hCtx) {
        Object rs = global_class_service(hCtx);
        if (rs == null) {
            String className = hCtx.className();
            String actionName = hCtx.actionName();
            try {
                // 目标类不存在
                ReflectStruct _cls = getServiceApi(className);
                if (_cls != null) {
                    // 执行转换前置类
                    Object[] _objs = convert2GscCode(hCtx.invokeParamter());
                    FilterReturn filterReturn = beforeExecute(className, actionName, _objs);
                    if (filterReturn.state()) {
                        // 通过ioc方式获取类（复用已存在实例容器提高性能）
                        try (var obj = _reflect.build(_cls)) {
                            rs = obj._call(actionName, _objs);
                        } catch (Exception e) {
                            nLogger.logInfo(e, "实例化 " + _cls + " ...失败");
                        }
                        // 函数执行返回 null,尝试从代理服务获得数据
                        if (rs == null) {
                            rs = redirectRequest(hCtx);
                        }
                        // 尾过滤
                        rs = RpcResult(afterExecute(className, actionName, _objs, rs));
                        // 如果当前请求是更新操作,尝试向所有订阅者广播更新通知
                        if (ServiceApiClass.isUpdateAction(actionName)) {
                            GscSubscribe.update(getServiceTopic(hCtx), hCtx.appId());
                        }
                    } else {
                        rs = RpcMessage.Instant(filterReturn);
                    }
                } else {
                    rs = redirectRequest(hCtx);
                }
            } catch (Exception e) {
                nLogger.logInfo(e, "类:" + className + " : 不存在");
            }
        }
        return rs;
    }

    private static Object GscString2Object(Object o) {
        if (o instanceof String v) {
            var header = GscEncrypt.getHeader(v);
            if (header != null) {
                return switch (GscEncrypt.getType(header)) {
                    case "json" -> GscEncrypt.decodeJson(v);
                    case "jsonArray" -> GscEncrypt.decodeJsonArray(v);
                    case "string" -> GscEncrypt.decodeString(v);
                    default -> v;
                };
            }
            return v;
        } else if (o instanceof JSONObject m) {
            m.replaceAll((k, v) -> GscString2Object(v));
            return m;
        } else if (o instanceof JSONArray a) {
            a.replaceAll(ExecRequest::GscString2Object);
            return a;
        }
        return o;
    }

    // 转换 GscJson 参数(请求层转换)
    private static Object[] convert2GscCode(Object[] objs) {
        if (objs == null) {
            return null;
        }
        for (int i = 0; i < objs.length; i++) {
            Object o = objs[i];
            objs[i] = GscString2Object(o);
        }
        return objs;
    }

    // 过滤函数改变输入参数
    private static FilterReturn beforeExecute(String className, String actionName, Object[] objs) {
        // String classFullName = ExecBaseFolder + "._Before." + className;
        RpcFilterFnCache filterFn = getServiceBefore(className); // BeforeFilterObjectCache.getOrDefault(className, null);
        if (filterFn == null) {  // 没有过滤函数
            return FilterReturn.success();
        }
        try {
            return filterFn.filter(actionName, objs);
        } catch (Exception e) {
            return FilterReturn.build(false, "过滤函数异常");
        }
    }

    // 结果函数改变输入参数
    private static Object afterExecute(String className, String actionName, Object[] parameter, Object obj) {
        // String classFullName = ExecBaseFolder + "._After." + className;
        RpcFilterFnCache filterFn = getServiceAfter(className); // AfterFilterObjectCache.getOrDefault(className, null);
        if (filterFn == null) {  // 没有过滤函数
            return obj;
        }
        try {
            return filterFn.filter(actionName, parameter, obj);
        } catch (Exception e) {
            return obj;
        }
    }

    private static Object RpcResult(Object o) {
        if (o == null) {
            return rMsg.netState(false);
        }
        if (o instanceof RpcPure v) {
            return v.payload();
        }
        if (o instanceof String || o instanceof Integer || o instanceof List<?> || o instanceof HashMap<?, ?> || o instanceof Long || o instanceof Float || o instanceof Double || o instanceof Short || o instanceof Boolean
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

    // 字符串是否需要自动变加密编码
    private static String rpc_parameter2string(Object obj) {
        if (obj instanceof JSONObject) {
            return GscEncrypt.encodeJson((JSONObject) obj);
        } else if (obj instanceof JSONArray) {
            return GscEncrypt.encodeJsonArray((JSONArray) obj);
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