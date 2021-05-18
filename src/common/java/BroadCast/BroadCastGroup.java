package common.java.BroadCast;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.concurrent.ConcurrentHashMap;

public class BroadCastGroup {
    private final ConcurrentHashMap<String, Channel> group;
    private int idx;

    public BroadCastGroup() {
        idx = 0;
        group = new ConcurrentHashMap<>();
    }

    public BroadCastGroup add(Channel wsChannel) {
        String key = "g:" + idx;
        idx++;
        return add(key, wsChannel);
    }

    public BroadCastGroup add(String key, Channel wsChannel) {
        if (!group.containsValue(wsChannel)) {

            group.put(key, wsChannel);
        }
        return this;
    }

    public BroadCastGroup remove(String key) {
        group.remove(key);
        return this;
    }

    public BroadCastGroup remove(Channel wsChannel) {
        String _key = null;
        for (String key : group.keySet()) {
            if (key != null && group.get(key).equals(wsChannel)) {
                _key = key;
                break;
            }
        }
        if (_key != null) {
            group.remove(_key);
        }
        return this;
    }

    /**
     * 广播数据
     *
     * @param val
     */
    public void broadCast(Object val) {
        Channel chx;
        for (String key : group.keySet()) {
            if (key != null) {
                chx = group.get(key);
                if (chx.isWritable()) {
                    if (val instanceof String) {
                        chx.writeAndFlush(new TextWebSocketFrame((String) val));
                    }
                } else {
                    remove(chx);
                }
            }
        }
    }
}
