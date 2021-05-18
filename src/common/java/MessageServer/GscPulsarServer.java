package common.java.MessageServer;

import common.java.Config.Config;
import common.java.HttpServer.GrapeHttpServer;
import common.java.HttpServer.HttpContext;
import common.java.nLogger.nLogger;
import org.apache.pulsar.client.api.*;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GscPulsarServer {
    // pulsar 模式下, bindIP,Port 属性代表 pulsar集群连接信息
    // 当前服务所属所有部署服务ID和应用ID合成 主题组，负责消费
    // 通过服务通讯协议名定义，支持pulsar的单点消费，广播消费

    private final static ExecutorService service = Executors.newCachedThreadPool();
    //Pulsar集群中broker的serviceurl
    private static final String brokerServiceurl = "pulsar://" + Config.bindIP + ":" + Config.port;

    // topicName 主题名， Mode 模式 0 独占，1 共享， 2 灾备
    private static Consumer getConsumer(String topicName, int Mode) {
        //构造Pulsar client
        PulsarClient client = null;
        try {
            client = PulsarClient.builder()
                    .serviceUrl(brokerServiceurl)
                    .build();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }

        SubscriptionType subscriptionType;
        switch (Mode) {
            case 1:
                subscriptionType = SubscriptionType.Shared;
                break;
            case 2:
                subscriptionType = SubscriptionType.Failover;
            default:
                subscriptionType = SubscriptionType.Exclusive;
        }

        //创建consumer
        try {
            return client.newConsumer()
                    .topic(topicName)
                    .subscriptionName(topicName)
                    .subscriptionType(subscriptionType)//指定消费模式，包含：Exclusive，Failover，Shared，Key_Shared。默认Exclusive模式
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)//指定从哪里开始消费，还有Latest，valueof可选，默认Latest
                    .negativeAckRedeliveryDelay(60, TimeUnit.SECONDS)//指定消费失败后延迟多久broker重新发送消息给consumer，默认60s
                    .subscribe();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void start(JSONArray<JSONObject> arr) {
        try {
            // 此时订阅全部用到的数据
            for (JSONObject v : arr) {
                int appId = v.getInt("appId");
                String serviceName = v.getString("name");
                String topicName = "persistent://public/default/" + serviceName + "_" + appId;
                int mode = v.getInt("mode");
                service.submit(() -> {
                    var consumer = getConsumer(topicName, mode);
                    //消费消息
                    while (!Thread.currentThread().isInterrupted()) {
                        Message message = null;
                        try {
                            message = consumer.receive();
                        } catch (PulsarClientException e) {
                            e.printStackTrace();
                            break;
                        }
                        try {
                            JSONObject cmd = JSONObject.build(new String(message.getData()));
                            if (!JSONObject.isInvalided(cmd)) {
                                HttpContext ctx = new HttpContext(cmd);
                                GrapeHttpServer.EventLoop(ctx); // 不管结果
                            }
                            consumer.acknowledge(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                            consumer.negativeAcknowledge(message);
                        }
                    }
                });
            }
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception e) {
            nLogger.errorInfo(e);
        } finally {
            nLogger.logInfo("服务器关闭");
        }
    }
}
