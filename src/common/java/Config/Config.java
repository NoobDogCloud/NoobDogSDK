package common.java.Config;

import common.java.MasterProxy.MasterActor;
import common.java.String.StringHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class Config {
    public static final String masterId_Key = "MasterId";
    public static final String masterPass_Key = "MasterPass";
    private static final String configNodePath = "configs";
    public static String masterHost;
    public static int masterPort;
    public static String bindIP;
    public static int port;
    public static boolean debug;
    public static String publicKey;
    public static String masterId;
    public static String masterPass;

    public static String serviceName;
    private static String configPath = "gfw.cfg";
    private static MasterActor configs;

    static {
        updateConfig();
        configs = null;
    }

    private static void setConfigPath(String newConfigPath) {
        configPath = newConfigPath;
    }

    private static MasterActor getNetConfigHandle() {
        if (configs == null) {
            configs = MasterActor.getInstance(configNodePath);
        }
        return configs;
    }

    private static Properties loadProps() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(configPath));
        } catch (IOException e) {
            // 现在不一定需要配置文件
        }
        return prop;
    }

    private static void WriteProps(Properties prop) {
        try (FileOutputStream fos = new FileOutputStream(configPath)) {
            try (OutputStreamWriter opw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                prop.store(opw, "update");
            } catch (IOException e) {
                nLogger.logInfo(e, "配置文件[" + configPath + "] 写入异常");
            }
        } catch (IOException e) {
            nLogger.logInfo(e, "配置文件[" + configPath + "]不存在");
        }
    }

    public static String getServiceName() {
        return serviceName;
    }

    public static void updateConfig() {
        Properties prop = loadProps();
        // 必选项
        port = Integer.parseInt(prop.getProperty("port", "805"));
        // 附加选项
        serviceName = prop.getProperty("name", "system");
        // 可选项
        masterHost = prop.getProperty("MasterHost", "127.0.0.1");//read master host url
        masterPort = Integer.parseInt(prop.getProperty("MasterPort", "80"));
        bindIP = prop.getProperty("bindIP", "0.0.0.0");//本地服务节点通信Ip
        debug = Boolean.parseBoolean(prop.getProperty("Debug", "true"));
        publicKey = prop.getProperty("publicKey", "grapeSoft@");

        masterId = prop.getProperty(masterId_Key);
        masterPass = prop.getProperty(masterPass_Key);
    }

    public static String getMasterUrl() {
        return masterHost + ":" + masterPort;
    }

    public static String getNodeID() {
        return bindIP + "_" + port;
    }

    // 通过masterProxy从数据库获得配置信息
    public static String netConfig(String session) {
        JSONArray<JSONObject> rs = getNetConfigHandle().getDataByIndex("name", session);
        if (JSONArray.isInvalided(rs)) {
            throw new RuntimeException("配置[" + session + "] ->不存在!");
        }
        return rs.get(0).getJson("config").toString();
    }

    public static void set(String key, Object val) {
        Properties prop = loadProps();
        prop.setProperty(key, StringHelper.toString(val));
        WriteProps(prop);
        updateConfig();
    }

    public static String config(String session) {
        return loadProps().getProperty(session);
    }
}