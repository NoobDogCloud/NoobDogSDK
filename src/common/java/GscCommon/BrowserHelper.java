package common.java.GscCommon;

import common.java.HttpServer.HttpContext;

import java.util.Locale;

public class BrowserHelper {
    // MicroMessenger
    public static boolean isMicroMessenger() {
        String agent = HttpContext.current().agent();
        return (agent != null && agent.contains("MicroMessenger"));
    }

    // AlipayClient
    public static boolean isAlipayClient() {
        String agent = HttpContext.current().agent();
        return (agent != null && agent.contains("AlipayClient"));
    }

    // 爬虫
    public static boolean isCrawler() {
        // 百度爬虫
        String agent = HttpContext.current().agent().toLowerCase(Locale.ROOT);
        if (agent.startsWith("baidu")) {
            return true;
        }
        // 一搜
        if (agent.startsWith("yisou")) {
            return true;
        }
        // 谷歌
        if (agent.startsWith("google")) {
            return true;
        }
        if (agent.startsWith("ads")) {
            return true;
        }
        // 搜狗
        if (agent.startsWith("sogou")) {
            return true;
        }
        // 360
        if (agent.startsWith("360")) {
            return true;
        }
        if (agent.startsWith("haosou")) {
            return true;
        }
        // 必应
        if (agent.startsWith("bing")) {
            return true;
        }
        // 搜搜
        if (agent.startsWith("soso")) {
            return true;
        }
        // 雅虎
        return agent.startsWith("yahoo");
    }
}
