package common.java.Http.Common;

import common.java.String.StringHelper;
import io.netty.channel.ChannelId;

import java.util.HashMap;

public class RequestSession {
    private final static HashMap<String, SocketContext> requestCache = new HashMap<>();

    public static SocketContext create(String cid) {
        SocketContext sc = SocketContext.build(cid);
        requestCache.put(cid, sc);
        return sc;
    }

    public static void remove(String cid) {
        requestCache.remove(cid);
    }

    public static SocketContext get(String cid) {
        return requestCache.get(cid);
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
