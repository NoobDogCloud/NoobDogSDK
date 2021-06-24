package common.java.Http.Server.Subscribe;

import common.java.Http.Server.HttpContext;
import common.java.Time.TimeHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Room {
    // 全局 房间池
    private static final ConcurrentHashMap<String, Room> room_pool = new ConcurrentHashMap<>();

    // 房间成员记录
    private final ConcurrentHashMap<ChannelId, Member> memberArr;
    // 房间主题
    private final String topic;
    // 主题包含新数据
    private final AtomicBoolean updateStatus = new AtomicBoolean(false);
    // 主题数据刷新时间
    private final AtomicLong updateTime = new AtomicLong(0);
    // 主题最后同步更新时间
    private final AtomicLong syncUpdateTime = new AtomicLong(0);
    // 主题最后广播时间
    private final AtomicLong broadcastTime = new AtomicLong(0);

    private Room(String Topic) {
        topic = Topic;
        memberArr = new ConcurrentHashMap<>();
    }

    public static void removeMember(ChannelId cid) {
        for (Room r : room_pool.values()) {
            r.leave(cid);
        }
    }

    public static void foreach(Consumer<Room> fn) {
        for (Room r : room_pool.values()) {
            fn.accept(r);
        }
    }

    public static Room getInstance(String Topic) {
        if (room_pool.containsKey(Topic)) {
            return room_pool.get(Topic);
        } else {
            Room room = new Room(Topic);
            room_pool.put(Topic, room);
            return room;
        }
    }

    public String getTopic() {
        return topic;
    }

    public boolean getUpdateStatus() {
        return updateStatus.get();
    }

    public long getUpdateTime() {
        return updateTime.get();
    }

    public Room fleshUpdateTime() {
        this.updateTime.set(TimeHelper.getNowTimestampByZero());
        return this;
    }

    public long getBroadcastTime() {
        return broadcastTime.get();
    }

    public Room fleshUpdateStatus() {
        this.updateStatus.set(true);
        return this;
    }

    private Room fleshBroadcastTime() {
        this.broadcastTime.set(TimeHelper.getNowTimestampByZero());
        this.updateStatus.set(false);
        return this;
    }

    public long getSyncUpdateTime() {
        return syncUpdateTime.get();
    }

    public Room fleshSyncUpdateTime() {
        this.syncUpdateTime.set(TimeHelper.getNowTimestampByZero());
        return this;
    }

    // 获得成员
    public Member member(ChannelHandlerContext ch) {
        ChannelId cid = ch.channel().id();
        return memberArr.get(cid);
    }

    // 加入成员
    public Member add(ChannelHandlerContext ch, HttpContext task) {
        ChannelId cid = ch.channel().id();
        if ((memberArr.containsKey(cid))) {
            memberArr.get(cid).setQueryTask(task);
        } else {
            memberArr.put(cid, Member.build(ch, task));
        }
        return memberArr.get(cid);
    }

    // 成员退出
    public Room leave(ChannelId cid) {
        memberArr.remove(cid);
        if (memberArr.size() == 0) {
            this.releaseRoom();
        }
        return this;
    }

    // 释放房间
    private void releaseRoom() {
        room_pool.remove(topic);
    }

    // 成员更新数据
    public void update() {
        for (Member m : memberArr.values()) {
            m.refresh();
        }
        this.fleshBroadcastTime();
    }
}
