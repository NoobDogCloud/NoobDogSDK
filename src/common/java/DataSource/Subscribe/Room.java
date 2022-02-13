package common.java.DataSource.Subscribe;

import common.java.Http.Common.SocketContext;
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
    // 应用id
    private final int appId;
    // 成员广播任务
    private Consumer<Member> refreshFunc;
    // 加入成员时 hook
    private Consumer<Member> joinFunc;
    // 离开成员时 hook
    private Consumer<Member> leaveFunc;
    // 房间销毁时 hook
    private Consumer<Room> destroyFunc;
    // 准备广播时 hook
    private Consumer<Room> broadcastFunc;

    private Room(String Topic, int appId) {
        topic = Topic;
        this.appId = appId;
        this.refreshFunc = null;
        memberArr = new ConcurrentHashMap<>();
    }

    public Room setJoinHook(Consumer<Member> joinFunc) {
        this.joinFunc = joinFunc;
        return this;
    }

    public Room setLeaveHook(Consumer<Member> leaveFunc) {
        this.leaveFunc = leaveFunc;
        return this;
    }

    public Room setBroadcastHook(Consumer<Room> broadcastFunc) {
        this.broadcastFunc = broadcastFunc;
        return this;
    }

    public Room setRoomDestroy(Consumer<Room> destroyFunc) {
        this.destroyFunc = destroyFunc;
        return this;
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

    public static Room getInstance(String Topic, int appId, DistributionSubscribeInterface distribution_subscribe) {
        String _topic = getTopicWithAppID(Topic, appId);
        if (room_pool.containsKey(_topic)) {
            return room_pool.get(_topic);
        } else {
            Room room = new Room(Topic, appId);
            room_pool.put(_topic, room);
            if (distribution_subscribe != null) {
                distribution_subscribe.pushStatus(room);
            }
            return room;
        }
    }

    public static String getTopicWithAppID(String topic, int appId) {
        return topic + "_" + appId;
    }

    public String getTopicWithAppID() {
        return topic + "_" + appId;
    }

    public String getTopic() {
        return topic;
    }

    public int getAppId() {
        return appId;
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
    public Member add(ChannelHandlerContext ch, SocketContext sCtx) {
        ChannelId cid = ch.channel().id();
        if ((memberArr.containsKey(cid))) {
            memberArr.get(cid).setSocketContext(sCtx);
        } else {
            Member member = Member.build(ch, sCtx).setRefreshFunc(refreshFunc);
            memberArr.put(cid, member);
            if (joinFunc != null) {
                joinFunc.accept(member);
            }
        }
        return memberArr.get(cid);
    }

    // 成员退出
    public Room leave(ChannelId cid) {
        memberArr.remove(cid);
        if (leaveFunc != null) {
            leaveFunc.accept(memberArr.get(cid));
        }
        if (memberArr.size() == 0) {
            this.releaseRoom();
        }
        return this;
    }

    // 释放房间
    private void releaseRoom() {
        if (destroyFunc != null) {
            destroyFunc.accept(this);
        }
        room_pool.remove(getTopicWithAppID());
    }

    // 成员更新数据
    public void update() {
        if (broadcastFunc != null) {
            broadcastFunc.accept(this);
        }
        for (Member m : memberArr.values()) {
            m.refresh();
        }
        this.fleshBroadcastTime();
    }

    // 修改成员数据更新默认方法
    public Room updateRefreshFunc(Consumer<Member> func) {
        if (func != null) {
            this.refreshFunc = func;
            for (Member m : memberArr.values()) {
                m.setRefreshFunc(func);
            }
        }
        return this;
    }
}
