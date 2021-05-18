package common.java.Coordination.Client;

import common.java.Config.Config;
import common.java.Coordination.Common.GscCenterPacket;
import common.java.Coordination.Common.MessagePacketDecoder;
import common.java.Coordination.Common.MessagePacketEncoder;
import common.java.Thread.ThreadHelper;
import common.java.nLogger.nLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpClient {
    private final GscCenterClient cli;
    private Bootstrap bootstrap;
    private EventLoopGroup group;
    /**
     * 客户端通道
     */
    private Channel clientChannel;
    private TCPClientHandler clientHandle;
    private Runnable onConnect;

    private TcpClient(GscCenterClient cli) {
        this.group = null;
        this.cli = cli;
        this.clientHandle = null;
        this.onConnect = null;
    }

    /**
     * @apiNote 获得TcpClient实例 ->单例模式
     */
    public static TcpClient build(GscCenterClient cli) {
        return new TcpClient(cli);
    }

    public TcpClient setConnected(Runnable fn) {
        this.onConnect = fn;
        return this;
    }

    private void init() {
        clientHandle = new TCPClientHandler(cli);
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        //自定义解码器
                        pipeline.addLast(new MessagePacketDecoder(TCPClientHandler.preload));
                        //自定义编码器
                        pipeline.addLast(new MessagePacketEncoder());
                        //自定义Handler
                        pipeline.addLast(clientHandle);
                    }
                });
    }

    private void _connect() {
        int err_no = 100;
        AtomicBoolean waitFlag = new AtomicBoolean(false);
        try {
            ChannelFuture channelFuture = bootstrap.connect(Config.masterHost, Config.masterPort);
            channelFuture.addListener((ChannelFutureListener) future -> {
                //如果连接成功
                if (future.isSuccess()) {
                    nLogger.errorInfo("客户端[" + channelFuture.channel().localAddress().toString() + "]已连接...");
                    clientChannel = channelFuture.channel();
                    this.cli.setLiveStatus(true);
                    this.cli.setKeepLived(true);
                }
                //如果连接失败，尝试重新连接
                else {
                    nLogger.errorInfo("客户端[" + channelFuture.channel().localAddress().toString() + "]连接失败，重新连接中...");
                    future.channel().close().channel().eventLoop().schedule((Runnable) this.cli::reConnect, 3, TimeUnit.SECONDS);
                    this.cli.setLiveStatus(false);
                }
                waitFlag.set(true);
            });
            while (!waitFlag.get() && err_no > 0) {
                ThreadHelper.sleep(100);
                err_no--;
            }
        } catch (Exception e) {
            nLogger.errorInfo(e, "连接GscCenter服务器失败!");
        }
    }

    public TcpClient run() {
        if (clientChannel == null) {
            init();
            _connect();
        }
        return this;
    }

    public void send(GscCenterPacket packet) {
        clientChannel.writeAndFlush(packet);
    }

    public TcpClient close() {
        System.out.println("客户端[关闭连接]!!!");
        //关闭客户端套接字
        if (clientChannel != null) {
            clientChannel.close();
            clientChannel = null;
        }
        //关闭客户端线程组
        if (group != null) {
            group.shutdownGracefully();
        }
        return this;
    }
}
