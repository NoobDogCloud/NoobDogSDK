package common.java.Http.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class GscServer {
    public static void start(String host, int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    //.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast(new HttpResponseEncoder());
                            ch.pipeline().addLast(new ChunkedWriteHandler());

                            ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/"));//升级协议到websocket

                            ch.pipeline().addLast(new NetEvents());//http,websocket服务
                        }
                    });


                    /*
                    .option(EpollChannelOption.SO_REUSEPORT, true);
                    .option(ChannelOption.SO_BACKLOG, 30000)
                    		.option(ChannelOption.TCP_NODELAY, false)
		                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_RCVBUF, 256 * 1024)
                    .option(ChannelOption.SO_RCVBUF, 256 * 1024);
                    */
            //.option(ChannelOption.SO_RCVBUF, 8192 * 1024 * 100000)
            //.option(ChannelOption.SO_SNDBUF, 8192 * 1024 * 100000)
            //.option(EpollChannelOption.SO_REUSEPORT, true)
            //.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            //.childOption(ChannelOption.SO_KEEPALIVE, false)
            //.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            try {
                ChannelFuture f = host != null ? b.bind(host, port).sync() : b.bind(port).sync();
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
