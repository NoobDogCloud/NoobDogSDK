package common.java.DataSource.Subscribe;

import common.java.Concurrency.HashmapTaskRunner;
import common.java.Http.Common.SocketContext;
import common.java.Http.Server.ApiSubscribe.GscSubscribe;
import common.java.Time.TimeHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Room {
    // 全局 房间池
    private static final HashmapTaskRunner<String, Room> room_pool = HashmapTaskRunner.<String, Room>getInstance((cid, room) -> {
        try {
            // 需要请求时上下文
            long n = TimeHelper.getNowTimestampByZero();
            // 包含需要更新数据
            if (GscSubscribe.getUpdateStatus(room)) {
                // 50ms未动 or 距离上次同步超过500ms => 推送同步数据时间戳
                if ((n - room.getUpdateTime() > 50) || (n - room.getSyncUpdateTime() > 500)) {
                    GscSubscribe._onChanged(room);
                }
                // 距离上次广播数据超过1000ms
                if (n - room.getBroadcastTime() > 5000) {
                    // 刷新房间内所有用户数据
                    room.update();
                }
            }
        } catch (Exception e) {
            // 删除订阅源,房间
            GscSubscribe.remove(room);
        }
    }).setDelay(50);
    // 每个房间最多人数
    private final int memberMax;
    // 房间成员记录
    private final ConcurrentHashMap<String, Member> memberArr = new ConcurrentHashMap<>();
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
    private final List<Consumer<Member>> refreshFunc = new ArrayList<>();
    // 加入成员时 hook
    private final List<Consumer<Member>> joinFunc = new ArrayList<>();
    // 离开成员时 hook
    private final List<Consumer<Member>> leaveFunc = new ArrayList<>();
    // 房间销毁时 hook
    private final List<Consumer<Room>> destroyFunc = new ArrayList<>();
    // 准备广播时 hook
    private final List<Consumer<Room>> broadcastFunc = new ArrayList<>();

    private Room(String Topic, int appId) {
        this.topic = Topic;
        this.appId = appId;
        this.memberMax = 100;
    }

    private Room(String Topic, int appId, int memberMax) {
        this.topic = Topic;
        this.appId = appId;
        this.memberMax = memberMax;
    }

    public Room setJoinHook(Consumer<Member> joinFunc) {
        this.joinFunc.add(joinFunc);
        return this;
    }

    public Room setLeaveHook(Consumer<Member> leaveFunc) {
        this.leaveFunc.add(leaveFunc);
        return this;
    }

    public Room setBroadcastHook(Consumer<Room> broadcastFunc) {
        this.broadcastFunc.add(broadcastFunc);
        return this;
    }

    public Room setRoomDestroy(Consumer<Room> destroyFunc) {
        this.destroyFunc.add(destroyFunc);
        return this;
    }

    public static void removeMember(ChannelId cid) {
        for (Room r : room_pool.values()) {
            r.leave(cid.asLongText());
        }
    }

    /*
    public static void foreach(Consumer<Room> fn) {
        for (Room r : room_pool.values()) {
            fn.accept(r);
        }
    }
    */

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
        String cid = ch.channel().id().asLongText();
        return memberArr.get(cid);
    }

    // 加入成员
    public Member add(ChannelHandlerContext ch, SocketContext sCtx) {
        String cid = ch.channel().id().asLongText();
        if ((memberArr.containsKey(cid))) {
            memberArr.get(cid).setSocketContext(sCtx);
        } else {
            Member member = Member.build(ch, sCtx);
            // 设置 member 广播行为是Room的行为
            if (refreshFunc.size() > 0) {
                member.setRefreshFunc(refreshFunc);
            }
            memberArr.put(cid, member);
            for (var func : joinFunc) {
                func.accept(member);
            }
            // 绑定当前订阅房间到 socket 上下文
            sCtx.putSubscriber(this);
        }
        return memberArr.get(cid);
    }

    // 成员退出
    public Room leave(String cid) {
        // 从socket 上下文中移除当前房间
        var sCtx = SocketContext.current();
        if (sCtx != null) {
            sCtx.removeSubscriber(this);
        }
        for (var func : leaveFunc) {
            func.accept(memberArr.get(cid));
        }
        // 从房间中移除成员
        memberArr.remove(cid);
        if (memberArr.size() == 0) {
            this.releaseRoom();
        }
        return this;
    }

    // 释放房间
    public void releaseRoom() {
        for (var func : destroyFunc) {
            func.accept(this);
        }
        room_pool.remove(getTopicWithAppID());
    }

    // 成员更新数据
    public void update() {
        for (var func : broadcastFunc) {
            func.accept(this);
        }
        for (Member m : memberArr.values()) {
            m.refresh();
        }
        this.fleshBroadcastTime();
    }

    // 修改成员数据更新默认方法
    public Room updateRefreshFunc(Consumer<Member> func) {
        if (func != null) {
            this.refreshFunc.add(func);
            for (Member m : memberArr.values()) {
                m.setRefreshFunc(func);
            }
        }
        return this;
    }

    public int getMemberCount() {
        return memberArr.size();
    }
}
