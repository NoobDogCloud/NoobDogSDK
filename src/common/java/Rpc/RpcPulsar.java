package common.java.Rpc;

import common.java.Config.Config;
import common.java.HttpServer.HttpContext;
import common.java.OAuth.oauthApi;
import common.java.String.StringHelper;
import org.apache.pulsar.client.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class RpcPulsar {
    private static final HashMap<String, PulsarClient> clientCache = new HashMap<>();
    private static final HashMap<String, Producer<byte[]>> producerCache = new HashMap<>();

    private static PulsarClient getPulsar(String brokerServiceUrl) {
        if (!clientCache.containsKey(brokerServiceUrl)) {
            try {
                var client = PulsarClient.builder().serviceUrl(brokerServiceUrl).build();
                clientCache.put(brokerServiceUrl, client);
            } catch (Exception e) {
                return null;
            }
        }
        return clientCache.get(brokerServiceUrl);
    }

    /**
     * 订阅主题名       服务名（部署的）_应用ID
     * pulsar连接信息   peerAddr
     */
    private static Producer<byte[]> getProducer(int appId, String hostUrl, String serviceName) {
        String brokerServiceUrl = "pulsar://" + hostUrl;
        String topicName = "persistent://public/default/" + serviceName + "_" + appId;
        if (!producerCache.containsKey(topicName)) {
            var cli = getPulsar(brokerServiceUrl);
            try {
                Producer<byte[]> producer = cli.newProducer()
                        .topic(topicName)
                        .enableBatching(true)//是否开启批量处理消息，默认true,需要注意的是enableBatching只在异步发送sendAsync生效，同步发送send失效。因此建议生产环境若想使用批处理，则需使用异步发送，或者多线程同步发送
                        .compressionType(CompressionType.LZ4)//消息压缩（四种压缩方式：LZ4，ZLIB，ZSTD，SNAPPY），consumer端不用做改动就能消费，开启后大约可以降低3/4带宽消耗和存储（官方测试）
                        .batchingMaxPublishDelay(10, TimeUnit.MILLISECONDS) //设置将对发送的消息进行批处理的时间段,10ms；可以理解为若该时间段内批处理成功，则一个batch中的消息数量不会被该参数所影响。
                        .sendTimeout(0, TimeUnit.SECONDS)//设置发送超时0s；如果在sendTimeout过期之前服务器没有确认消息，则会发生错误。默认30s，设置为0代表无限制，建议配置为0
                        .batchingMaxMessages(100000)//批处理中允许的最大消息数。默认1000
                        .maxPendingMessages(100000)//设置等待接受来自broker确认消息的队列的最大大小，默认1000
                        .blockIfQueueFull(true)//设置当消息队列中等待的消息已满时，Producer.send 和 Producer.sendAsync 是否应该block阻塞。默认为false，达到maxPendingMessages后send操作会报错，设置为true后，send操作阻塞但是不报错。建议设置为true
                        .roundRobinRouterBatchingPartitionSwitchFrequency(10)//向不同partition分发消息的切换频率，默认10ms，可根据batch情况灵活调整
                        .batcherBuilder(BatcherBuilder.DEFAULT)//key_Shared模式要用KEY_BASED,才能保证同一个key的message在一个batch里
                        .create();
                producerCache.put(topicName, producer);
            } catch (PulsarClientException e) {
                e.printStackTrace();
            }
        }
        return producerCache.get(topicName);
    }

    /**
     * @apiNote 包含参数的URL的使用
     */
    public static RpcResponse call(int run_no, String url, HttpContext ctx, boolean api_auth) {
        String[] strArr = url.split("/");
        Object[] args = Arrays.stream(strArr).skip(4).toArray();
        return call(StringHelper.join(strArr, "/", 0, 4), ctx, api_auth, args);
    }

    public static RpcResponse call(String path, HttpContext ctx, Object... args) {
        return call(path, ctx, false, args);
    }

    public static RpcResponse call(String path, HttpContext ctx, boolean api_auth, Object... args) {
        return call(path, ctx, api_auth, false, args);
    }

    public static RpcResponse call(String path, HttpContext ctx, boolean api_auth, boolean public_key, Object... args) {
        // 构造httpContent
        if (ctx == null) {
            ctx = HttpContext.current();
            if (ctx == null) {
                ctx = ctx.cloneTo();
            }
        }
        // 构造http协议rpc完整地址
        if (!path.toLowerCase().startsWith("http://")) {
            path = "http://" + path;
        }
        path = path.split("//")[1];

        String[] rArr = path.split("/");
        String host = rArr[0];
        path = StringHelper.join(rArr, "/", 1, -1);
        var pathAndArgs = path + (args != null ? ExecRequest.objects2string(args) : "");

        ctx.host(host);
        ctx.serviceName(rArr[1]);
        ctx.path(pathAndArgs);

        if (api_auth) {
            ctx.header().put(HttpContext.GrapeHttpHeader.token, oauthApi.getInstance().getApiToken(rArr[1] + "@" + rArr[2] + "@" + rArr[3]));
        }
        if (public_key) {
            ctx.header().put(HttpContext.GrapeHttpHeader.publicKey, Config.publicKey);
        }

        var pro = getProducer(ctx.appId(), ctx.host(), ctx.serviceName());
        var future = pro.sendAsync(ctx.toString().getBytes(StandardCharsets.UTF_8));
        future.handle((v, ex) -> {
            ex.printStackTrace();
            return null;
        });
        return RpcResponse.build(rMsg.netMSG(true));
    }
}
