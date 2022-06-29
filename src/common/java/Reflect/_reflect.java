package common.java.Reflect;

import common.java.Concurrency.ObjectPool;
import common.java.Encrypt.GscEncrypt;
import common.java.Http.Server.Upload.UploadFile;
import common.java.InterfaceModel.Type.InterfaceType;
import common.java.InterfaceModel.Type.InterfaceTypeArray;
import common.java.JGrapeSystem.SystemDefined;
import common.java.OAuth.oauthApi;
import common.java.Rpc.ExecRequest;
import common.java.Rpc.FilterCallback;
import common.java.Rpc.ReturnCallback;
import common.java.Rpc.RpcMessage;
import common.java.Session.UserSession;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class _reflect implements AutoCloseable {
    private static final HashMap<Class<?>, Class<?>> callType;

    static {
        callType = new HashMap<>();
        callType.put(String.class, null);
        callType.put(Integer.class, int.class);
        callType.put(Float.class, float.class);
        callType.put(Double.class, double.class);
        callType.put(Boolean.class, boolean.class);
        callType.put(Long.class, long.class);
        callType.put(char.class, null);
        callType.put(short.class, null);
        callType.put(JSONObject.class, null);
        callType.put(JSONArray.class, null);
        callType.put(Function.class, null);
        callType.put(ArrayList.class, List.class);
        callType.put(Object.class, null);
        callType.put(File.class, null);
        callType.put(UploadFile.class, null);
        callType.put(byte.class, null);
        callType.put(byte[].class, null);
        callType.put(InputStream.class, null);
        callType.put(FileInputStream.class, null);
        callType.put(ByteArrayInputStream.class, null);
        callType.put(OutputStream.class, null);
        callType.put(FileOutputStream.class, null);
        callType.put(ByteArrayOutputStream.class, null);
    }

    private static final HashMap<Class<?>, ObjectPool<_reflect>> ContainerPool = new HashMap<>();

    private final Class<?> _Class;        //类
    private final MethodAccess _method;
    private final ReflectStruct _rf;
    private final HashMap<String, FilterCallback> filterCallback; //调用前接口HOOK数组
    private final HashMap<String, ReturnCallback> returnCallback; //调用后接口HOOK数组
    private Object _oObject;        //对象
    private boolean _superMode;     //私有接口是否生效(是否允许调用 private 接口)
    private boolean privateMode;    //注解解析是否生效(是否按照接口注解定义控制访问)

    private _reflect(ReflectStruct cls) {
        // 初始化属性
        filterCallback = new HashMap<>();
        returnCallback = new HashMap<>();
        _superMode = false;
        privateMode = false;
        _Class = cls.getCls();
        _method = cls.getMethod();
        _rf = cls;
    }

    public static _reflect build(ReflectStruct cls) {
        Class<?> c = cls.getCls();
        if (!ContainerPool.containsKey(c)) {
            ContainerPool.put(c, ObjectPool.build(() -> {
                try (var r = new _reflect(cls)) {
                    return cls.getMagicClassName() == null ? r.newInstance() : r.newInstance(cls.getMagicClassName());
                }
            }));
        }
        return ContainerPool.get(c).get();
    }

    public void close() {
        ContainerPool.get(_rf.getCls()).back(this);
    }

    // api接口类型文本化
    private static String declApiType(InterfaceType _at) {
        return switch (_at.value()) {
            case CloseApi -> "CloseApi";
            case OauthApi -> "OAuth2Api";
            case PrivateApi -> "PrivateApi";
            case SessionApi -> "SessionApi";
            default -> "PublicApi";
        } + ",";
    }

    /**
     * 获得当前类全部公开方法和参数的api声明
     *
     */
    public static Object getServDecl(Class<?> cls) {
        return _reflect.ServDecl(cls);
    }

    private static String AnnotationMethod(Method method) {
        StringBuilder apiString = new StringBuilder();
        Annotation[] atApiTypes = method.getDeclaredAnnotations();
        for (Annotation an : atApiTypes) {
            if (an.annotationType() == InterfaceTypeArray.class) {
                InterfaceTypeArray at = method.getAnnotation(InterfaceTypeArray.class);
                for (InterfaceType _at : at.value()) {
                    apiString.append(declApiType(_at));
                }
            } else if (an.annotationType() == InterfaceType.class) {
                apiString.append(declApiType(method.getAnnotation(InterfaceType.class)));
            }
        }
        return StringHelper.build(apiString.toString()).trimTrailingFrom(',').toString();
    }

    public static String ParameterMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        StringBuilder ParamString = new StringBuilder();
        for (final Parameter parameter : parameters) {
            //生成参数字符串
            // ParamString.append(ExecRequest.class2string(parameter.getType())).append(":").append(parameter.getName()).append(",");
            ParamString.append(ExecRequest.class2string(parameter.getType())).append(",");
        }
        String param = ParamString.toString();
        return StringHelper.isInvalided(param) ? "" : StringHelper.build(param).trimFrom(',').toString();
    }


    // private static final HashMap<String, JSONArray<JSONObject>> ServDeclCache = new HashMap<>();
    private static final JSONObject ServDeclCache = JSONObject.build();

    private static Object ServDeclAll() {
        return ServDeclCache;
    }

    private static Object ServDecl(Class<?> cls) {
        String clsName = cls.getName();
        JSONArray<JSONObject> func = ServDeclCache.getJsonArray(clsName);
        if (func == null) {
            func = new JSONArray<>();
            Method[] methods;
            do {
                methods = cls.getDeclaredMethods();
                for (Method method : methods) {
                    /* mf = 1  public
                     * mf = 9  public static
                     * mf = 25 public static final
                     * */
                    if (method.getModifiers() == Modifier.PUBLIC) {
                        String api = AnnotationMethod(method);
                        if (!api.contains("CloseApi")) {
                            String _param = ParameterMethod(method);
                            String _name = method.getName();
                            boolean existing = false;
                            for (var f : func) {
                                if (f.getString("name").equals(_name) && f.getString("param").equals(_param)) {
                                    existing = true;
                                    break;
                                }
                            }
                            if (!existing) {
                                if ("".equals(api)) {
                                    api = "PublicApi";
                                }
                                func.add(JSONObject.build("name", _name)
                                        .put("level", api)
                                        .put("param", _param));
                            }
                        }
                    }
                }
                cls = cls.getSuperclass();
            } while (cls != Object.class);
            ServDeclCache.put(clsName, func);
        }
        return func;
    }

    public _reflect superMode() {
        _superMode = true;
        return this;
    }

    public _reflect newInstance(Object... parameters) {
        List<Object> pArr = new ArrayList<>();
        Class<?>[] cls = c2p(parameters, pArr);
        // 初始化反射类
        try {
            var cObject = _Class.getDeclaredConstructor(cls);
            _oObject = pArr.size() > 0 ? cObject.newInstance(pArr.toArray()) : cObject.newInstance();
        } catch (InstantiationException e) {
            nLogger.logInfo(e, "初始化类:" + _Class.getName() + " 实例化失败");
            _oObject = null;
        } catch (IllegalAccessException e) {
            nLogger.logInfo(e, "初始化类:" + _Class.getName() + " 访问异常");
            _oObject = null;
        } catch (IllegalArgumentException e) {
            nLogger.logInfo(e, "初始化类:" + _Class.getName() + " 无效参数");
            _oObject = null;
        } catch (InvocationTargetException e) {
            nLogger.logInfo(e, "初始化类:" + _Class.getName() + " 无效调用");
            _oObject = null;
        } catch (NoSuchMethodException e) {
            nLogger.logInfo(e, "初始化类:" + _Class.getName() + " 方法不存在");
            _oObject = null;
        } catch (SecurityException e) {
            nLogger.logInfo(e, "初始化类:" + _Class.getName() + " acl异常");
            _oObject = null;
        }
        return this;
    }

    public _reflect filterInterface(List<String> actionNameArray, FilterCallback cb) {
        for (String actionName : actionNameArray) {
            try (var ignored = filterInterface(actionName, cb)) {
            }
        }
        return this;
    }

    public _reflect filterInterface(String actionName, FilterCallback cb) {
        filterCallback.put(actionName, cb);
        return this;
    }

    public _reflect returnInterface(List<String> actionNameArray, ReturnCallback cb) {
        for (String actionName : actionNameArray) {
            try (var ignored = returnInterface(actionName, cb)) {
            }
        }
        return this;
    }

    public _reflect returnInterface(String actionName, ReturnCallback cb) {
        returnCallback.put(actionName, cb);
        return this;
    }

    /*
    private Method _getMethod(String functionName, Class<?>[] parameterlist) {
        int i = parameterlist == null ? 0 : parameterlist.length;
        Class<?>[] c = parameterlist == null ? new Class<?>[]{} : parameterlist;
        Method comMethod = null;
        while (true) {
            try {
                comMethod =  _Class.getMethod(functionName, c);
            } catch (NoSuchMethodException e) {
            }
            if (comMethod == null && i > 0) {
                i--;
                c[i] = Object.class;
            } else {
                break;
            }
        }
        return comMethod;
    }
     */

    private Class<?>[] object2class(Object[] parameters) {
        Class<?>[] rs = new Class[parameters.length];
        try {
            for (int i = 0; i < parameters.length; i++) {
                Object obj = parameters[i];
                Class<?> _vClass;
                Class<?> _oClass = obj.getClass();
                if (callType.containsKey(_oClass)) {
                    // 如果是字符串，可能是特殊 json,jsonArray
                    if (_oClass == String.class) {
                        var objStr = (String) obj;
                        var header = GscEncrypt.getHeader(objStr);
                        if (header != null) {
                            switch (GscEncrypt.getType(header)) {
                                case "json" -> {
                                    _oClass = JSONObject.class;
                                    obj = GscEncrypt.decodeJson(objStr);
                                }
                                case "json_array" -> {
                                    _oClass = JSONArray.class;
                                    obj = GscEncrypt.decodeJson(objStr);
                                }
                                case "string" -> obj = GscEncrypt.decodeString(objStr);
                            }
                            // 替换参数值
                            parameters[i] = obj;
                        }
                    }
                    // 如果是包装类，替换成纯数据类型
                    _vClass = callType.get(_oClass);
                    if (_vClass != null) {
                        _oClass = _vClass;
                    }
                } else {
                    //增补特殊形参
                    if (obj instanceof Function<?, ?>) {
                        _oClass = Function.class;
                    } else if (obj instanceof Consumer<?>) {
                        _oClass = Consumer.class;
                    } else if (obj instanceof Predicate<?>) {
                        _oClass = Predicate.class;
                    } else if (obj instanceof Supplier<?>) {
                        _oClass = Supplier.class;
                    } else {
                        _oClass = Object.class;
                    }
                }
                rs[i] = _oClass;
            }
        } catch (Exception e) {
            rs = null;
        }
        return rs;
    }

    public _reflect privateMode() {
        privateMode = true;
        return this;
    }

    /**
     * @apiNote 全局的系统服务
     */
    private Object global_service(String functionName, Object... parameters) {
        Object rs = null;
        try {
            if ("@description".equalsIgnoreCase(functionName)) {
                rs = ServDecl(_Class);
            } else if ("@descriptionAll".equalsIgnoreCase(functionName)) {
                rs = ServDeclAll();
            }
        } catch (Exception e) {
            rs = "系统服务[" + functionName + "]异常";
        }
        return rs;
    }

    private RpcMessage chkApiType(Object _at) {
        RpcMessage rs = null;
        InterfaceType.type v = null;
        if (_at instanceof InterfaceType.type) {
            v = (InterfaceType.type) _at;
        } else if (_at instanceof InterfaceType at) {
            v = at.value();
        }
        if (v != null) {
            switch (v) {
                case SessionApi:
                    if (!UserSession.current().checkSession()) {//会话不存在
                        rs = RpcMessage.Instant(SystemDefined.interfaceSystemErrorCode.SessionApi, "当前请求不在有效会话上下文内");
                    }
                    break;
                case OauthApi:
                    if (!oauthApi.getInstance().checkApiToken()) {
                        rs = RpcMessage.Instant(SystemDefined.interfaceSystemErrorCode.OauthApi, "当前token无效或已过期");
                    }
                    break;
                case CloseApi:
                    rs = RpcMessage.Instant(SystemDefined.interfaceSystemErrorCode.CloseApi, "非法接口");
                    break;
                case PrivateApi:
                    rs = _superMode ? null : RpcMessage.Instant(SystemDefined.interfaceSystemErrorCode.PrivateApi, "内部接口");
                    break;
                default:
                    break;
            }
        }
        return rs;
    }

    // 检查方法注解
    private Object chkAnnotationAction(String functionName) {
        //------------------方法注解检查，多个注解权限时，OR逻辑连接多个注解条件
        Object rs = null;
        if (!privateMode) {
            AnnotationStruct[] ans = _rf.getMethodAnnotation(functionName);
            if (ans != null) {
                for (AnnotationStruct an : ans) {//遍历全部注解
                    if (an.getType() == InterfaceType.class) {
                        rs = chkApiType(an.getVal());
                    } else if (an.getType() == InterfaceTypeArray.class) {
                        InterfaceTypeArray atApiType = an.getVal();
                        for (InterfaceType _at : atApiType.value()) {
                            rs = chkApiType(_at);
                            if (rs == null) {
                                break;
                            } else {
                                rs = "Interface Error:[" + rs + "]";
                            }
                        }
                    }
                }
            }
        }
        return rs;
    }

    // 调用主方法
    private Object callMainAction(String functionName, Object... parameters) {
        // 方法注解检查
        Object rs = chkAnnotationAction(functionName);
        if (rs != null) {
            return rs;
        }
        //------------------方法类型检查
        // int mf = comMethod.getModifiers();
        /* mf = 1  public
         * mf = 9  public static
         * mf = 25 public static final
         * */
        try {
            // 如果包含调用hook
            // 调用方法
            return _method.invoke(_oObject, functionName, parameters);
            // 如果包含结果hook
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            nLogger.logInfo(e, _Class.getName() + "." + functionName + "无效参数");
        } catch (Exception e) {
            nLogger.logInfo(e, "函数:" + _Class.getName() + "." + functionName + "未知异常");
        }
        return null;
    }

    private Class<?>[] objectArr2class(Object... parameters) {
        if (parameters instanceof String[]) {
            return new Class[]{String[].class};
        } else if (parameters instanceof Integer[]) {
            return new Class[]{Integer[].class};
        } else if (parameters instanceof Long[]) {
            return new Class[]{Long[].class};
        } else if (parameters instanceof Float[]) {
            return new Class[]{Float[].class};
        } else if (parameters instanceof Double[]) {
            return new Class[]{Double[].class};
        } else if (parameters instanceof Short[]) {
            return new Class[]{Short[].class};
        } else if (parameters instanceof Byte[]) {
            return new Class[]{Byte[].class};
        } else {
            return null;
        }
    }

    private Class<?>[] c2p(Object[] parameters, List<Object> out) {
        Class<?>[] cls;
        if (parameters == null || parameters.length == 0) {
            return null;
        }
        cls = objectArr2class(parameters);
        if (cls == null) {
            cls = object2class(parameters);
            out.addAll(Arrays.asList(parameters));
        } else {
            out.add(parameters);
        }
        return cls;
    }

    /**
     * 反射调用类方法
     * 补充功能，此处获得方法apiType注解信息，判断其接口属性，决定是否有返回值
     *
     * @param functionName 方法名
     * @param parameters   参数
     * @return 返回值
     */
    public Object _call(String functionName, Object... parameters) {
        Object rs = global_service(functionName, parameters);
        if (rs == null) {
            // 调用主要方法
            rs = callMainAction(functionName, parameters);
        }
        _superMode = false;
        return rs;
    }
}
