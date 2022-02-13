package common.java.DataSource.Subscribe;

import common.java.Http.Common.SocketContext;
import common.java.Http.Server.GrapeHttpServer;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;

import java.util.function.Consumer;


public class Member {
    private ChannelHandlerContext ch;               // 通讯对象
    private SocketContext socketContext;                  // 查询任务
    private Consumer<Member> refreshTask;                   // 刷新任务

    private Member(ChannelHandlerContext ch, SocketContext socketContext) {
        this.ch = ch;
        var ctx = socketContext.getRequest();
        JSONObject h = ctx.header();
        // 存留任务时，删除 mode 头字段，防止重复订阅
        h.remove("mode");
        this.socketContext = socketContext;
        this.refreshTask = m -> {
            GrapeHttpServer._startService(m.ch, m.socketContext.getRequest());
        };
    }

    public static Member build(ChannelHandlerContext ch, SocketContext socketContext) {
        return new Member(ch, socketContext);
    }

    public Member setRefreshFunc(Consumer<Member> func) {
        if (func != null) {
            refreshTask = func;
        }
        return this;
    }

    public ChannelHandlerContext getCh() {
        return ch;
    }

    public void setCh(ChannelHandlerContext ch) {
        this.ch = ch;
    }

    public SocketContext getSocketContext() {
        return socketContext;
    }

    public void setSocketContext(SocketContext socketContext) {
        this.socketContext = socketContext;
    }

    public void refresh() {
        refreshTask.accept(this);
    }

    public void send(String topic, Object msg) {
        socketContext.getResponse().out(
                GrapeHttpServer.WebsocketResult(topic, msg)
        );
    }
}
