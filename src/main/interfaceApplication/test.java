package main.interfaceApplication;

import common.java.JGrapeSystem.GscBooster;
import org.json.gsc.JSONObject;

public class test {
    public static void main(String[] args) {

        System.out.println("-");
        System.out.println("GrapeFW!");
        String[] packs = test.class.getPackage().getName().split("\\.");
        //System.setProperty("-Dco.paralleluniverse.fibers.detectRunawayFibers", String.valueOf(false));
        System.out.println("启动服务...");
        try {
            GscBooster.start("测试服务");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("服务器崩溃!");
        }
        System.out.println("-");
        /*
         * localdb
         * localcache
         * 2个配置必须存在而且有效
         * */
    }

    public JSONObject test(String a, int b) {
        return (new JSONObject()).put("测试信息", a).put("测试参数", b);
    }
}
