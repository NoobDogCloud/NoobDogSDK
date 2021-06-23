package common.java.Http.Server.Subscribe;

import common.java.Http.Server.GrapeHttpServer;
import common.java.Http.Server.HttpContext;
import io.netty.channel.ChannelHandlerContext;
import org.json.gsc.JSONObject;


public class Member {
    private ChannelHandlerContext ch;             // 通讯对象
    private HttpContext queryTask;  // 查询任务
    private String token;           // 任务唯一识别符

    private Member(ChannelHandlerContext ch, HttpContext task) {
        this.ch = ch;
        JSONObject h = task.header();
        // 存留任务时，删除 mode 头字段，防止重复订阅
        h.remove("mode");
        this.queryTask = task;
    }

    public static Member build(ChannelHandlerContext ch, HttpContext task) {
        return new Member(ch, task);
    }

    public ChannelHandlerContext getCh() {
        return ch;
    }

    public void setCh(ChannelHandlerContext ch) {
        this.ch = ch;
    }

    public HttpContext getQueryTask() {
        return queryTask;
    }

    public void setQueryTask(HttpContext queryTask) {
        this.queryTask = queryTask;
    }

    public String getToken() {
        return token;
    }

    public void refresh() {
        GrapeHttpServer._startService(ch, queryTask);
    }
}
