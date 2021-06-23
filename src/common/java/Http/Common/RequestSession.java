package common.java.Http.Common;

import common.java.Reflect._reflect;
import common.java.String.StringHelper;
import io.netty.channel.ChannelId;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RequestSession {
    private final static HashMap<ChannelId, ConcurrentHashMap<String, Object>> requestCache = new HashMap<>();
    private final static ThreadLocal<ChannelId> channelID = new ThreadLocal<>();
    private final static ThreadLocal<_reflect> currentClass = new ThreadLocal<>();

    public static void setCurrent(_reflect _currentClass) {
        currentClass.set(_currentClass);
    }

    public static _reflect getCurrentClass() {
        return currentClass.get();
    }

    public static void create(ChannelId cid) {
        requestCache.put(cid, new ConcurrentHashMap<>());
    }

    public static <T> T getValue(String key) {
        try {
            return (T) requestCache.get(channelID.get()).get(key);
        } catch (Exception e) {
        }
        return null;
    }

    public static <T> void setValue(String key, T val) {
        ChannelId cid = channelID.get();
        if (cid == null) {
            cid = buildChannelId();
            setChannelID(cid);
        }
        var hash = requestCache.get(cid);
        if (hash == null) {
            create(cid);
        }
        requestCache.get(cid).put(key, val);
    }

    public static void remove(ChannelId cid) {
        requestCache.remove(cid);
    }

    public static ChannelId getChannelID() {
        return channelID.get();
    }

    public static void setChannelID(ChannelId cid) {
        channelID.set(cid);
    }

    public static ChannelId buildChannelId() {
        return new ChannelId() {
            private final String shortText = StringHelper.createRandomCode(6);
            private final String longText = shortText + "_" + StringHelper.createRandomCode(6);

            @Override
            public String asShortText() {
                return "v_" + shortText;
            }

            @Override
            public String asLongText() {
                return "vl_" + longText;
            }

            @Override
            public int compareTo(ChannelId o) {
                return o.asLongText().equalsIgnoreCase(this.asLongText()) ? 0 : 1;
            }
        };
    }
}
