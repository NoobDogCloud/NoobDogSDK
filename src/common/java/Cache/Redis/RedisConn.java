package common.java.Cache.Redis;

import common.java.Cache.Cache;
import common.java.Encrypt.Md5;
import common.java.nLogger.nLogger;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import org.json.gsc.JSONObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RedisConn {
    private Object conn;

    private RedisConn(JSONObject config) {
        String key = Md5.build(config.toString());
        if (Cache.CacheClient.containsKey(key)) {
            this.conn = Cache.CacheClient.get(key);
        } else {
            if (config.getString("single") != null) {
                this.conn = connSingle(config);
            } else if (config.getString("sentinel") != null) {
                this.conn = connSentinel(config);
            } else if (config.getString("masterslave") != null) {
                this.conn = connMasterSlave(config);
            } else if (config.getString("cluster") != null) {
                this.conn = connCluster(config);
            }
            if (this.conn != null) {
                Cache.CacheClient.put(key, this.conn);
            }
        }
    }

    public static RedisConn build(String config) {
        JSONObject configJson = JSONObject.toJSON(config);
        if (JSONObject.isInvalided(configJson)) {
            nLogger.errorInfo("配置信息格式错误 ->[" + config + "]");
        }
        return new RedisConn(configJson);
    }

    public static RedisConn build(JSONObject config) {
        return new RedisConn(config);
    }

    public Object getConnect() {
        return this.conn;
    }

    public RedisConn close() {
        return this;
    }

    private List<String> fixRedisURI(String uriString) {
        List<String> uriValues = Arrays.asList(uriString.split(":"));
        if (uriValues.size() == 1) {
            uriValues.add(String.valueOf(RedisURI.DEFAULT_REDIS_PORT));
        }
        return uriValues;
    }

    private StatefulRedisConnection<String, String> connSingle(JSONObject configs) {
        String uri = configs.getString("single");
        String[] uris = uri.split(":");
        List<String> uriValues = new ArrayList<>();
        Collections.addAll(uriValues, uris);
        if (uriValues.size() == 1) {
            uriValues.add(String.valueOf(RedisURI.DEFAULT_REDIS_PORT));
        }
        RedisURI.Builder redisBuilder = RedisURI.Builder.redis(uriValues.get(0), Integer.parseInt(uriValues.get(1)));
        redisBuilder.withDatabase(0);
        if (configs.containsKey("password")) {
            redisBuilder.withPassword(configs.getString("password").toCharArray());
        }
        if (configs.containsKey("ssl")) {
            redisBuilder.withSsl(configs.getBoolean("ssl"));
        }
        RedisClient client = RedisClient.create(redisBuilder.build());
        client.setOptions(
                ClientOptions.builder()
                        .autoReconnect(true)
                        .pingBeforeActivateConnection(true)
                        .build());
        return client.connect();
    }

    private StatefulRedisConnection<String, String> connSentinel(JSONObject configs) {
        String uris = configs.getString("sentinel");
        List<String> uriArray = Arrays.asList(uris.split(","));
        // List<RedisURI> links = new ArrayList<>();
        List<String> uriValues = fixRedisURI(uriArray.get(0));
        RedisURI.Builder redisBuilder = RedisURI.Builder.sentinel(uriValues.get(0), Integer.parseInt(uriValues.get(1)));
        for (int i = 0; i < uriArray.size(); i++) {
            List<String> _uriValues = fixRedisURI(uriArray.get(0));
            redisBuilder.withSentinel(_uriValues.get(0), Integer.parseInt(_uriValues.get(1)));
        }
        if (configs.containsKey("password")) {
            redisBuilder.withPassword(configs.getString("password").toCharArray());
        }
        if (configs.containsKey("ssl")) {
            redisBuilder.withSsl(configs.getBoolean("ssl"));
        }
        RedisClient client = RedisClient.create(redisBuilder.build());
        client.setOptions(
                ClientOptions.builder()
                        .autoReconnect(true)
                        .pingBeforeActivateConnection(true)
                        .build());
        return client.connect();
    }

    private StatefulRedisMasterReplicaConnection<String, String> connMasterSlave(JSONObject configs) {
        RedisClient redisClient = RedisClient.create();
        String uris = configs.getString("masterslave");
        List<String> uriArray = Arrays.asList(uris.split(","));
        List<String> uriValues = fixRedisURI(uriArray.get(0));
        RedisURI.Builder redisBuilder = null;
        if (uriArray.size() == 1) {//standalone mode
            redisBuilder = RedisURI.Builder.redis(uriValues.get(0), Integer.parseInt(uriValues.get(1)));
        }
        if (uriArray.size() > 1) {
            redisBuilder = RedisURI.Builder.sentinel(uriValues.get(0), Integer.parseInt(uriValues.get(1)));
            for (int i = 1; i < uriArray.size(); i++) {
                List<String> _uriValues = fixRedisURI(uriArray.get(0));
                redisBuilder.withSentinel(_uriValues.get(0), Integer.parseInt(_uriValues.get(1)));
            }
        }
        if (configs.containsKey("password")) {
            redisBuilder.withPassword(configs.getString("password").toCharArray());
        }
        if (configs.containsKey("ssl")) {
            redisBuilder.withSsl(configs.getBoolean("ssl"));
        }
        RedisURI rURI = redisBuilder.build();

        StatefulRedisMasterReplicaConnection<String, String> conn = MasterReplica.connect(redisClient, StringCodec.UTF8, rURI);
        conn.setReadFrom(ReadFrom.MASTER_PREFERRED);
        return conn;
    }

    private StatefulRedisClusterConnection<String, String> connCluster(JSONObject configs) {
        String uris = configs.getString("cluster");
        String[] uriArray = uris.split(",");
        List<RedisURI> links = new ArrayList<>();
        for (String uriString : uriArray) {
            List<String> uriValues = Arrays.asList(uriString.split(":"));
            if (uriValues.size() == 1) {
                uriValues.add(String.valueOf(RedisURI.DEFAULT_REDIS_PORT));
            }
            RedisURI.Builder redisBuilder = RedisURI.Builder.redis(uriValues.get(0), Integer.parseInt(uriValues.get(1)));
            redisBuilder.withDatabase(0);
            if (configs.containsKey("password")) {
                redisBuilder.withPassword(configs.getString("password").toCharArray());
            }
            if (configs.containsKey("ssl")) {
                redisBuilder.withSsl(configs.getBoolean("ssl"));
            }
            links.add(redisBuilder.build());
        }
        RedisClusterClient jdc = RedisClusterClient.create(links);
        //GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport.createGenericObjectPool(() -> jdc.connect(), new GenericObjectPoolConfig());

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .dynamicRefreshSources(true)
                .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT, ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30))
                .build();
        ClusterClientOptions cco = ClusterClientOptions.builder()
                .autoReconnect(true)
                .maxRedirects(1000)
                .topologyRefreshOptions(topologyRefreshOptions)
                .build();
        jdc.setOptions(cco);
        return jdc.connect();
    }
}
