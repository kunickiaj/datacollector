package com.streamsets.pipeline.stage.origin.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class TCPServer {
  private final int port;
  private final ChannelInboundHandler handler;

  public TCPServer(final int port, ChannelInboundHandler handler) {
    this.port = port;
    this.handler = handler;
  }

  public void run() throws InterruptedException {
    EventLoopGroup mainGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(mainGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
              socketChannel.pipeline().addLast(handler);
            }
          })
          .option(ChannelOption.SO_BACKLOG, 128)
          .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator()) // use on-heap buffers
          .childOption(ChannelOption.SO_KEEPALIVE, true);

      // Bind and start to accept incoming connections
      ChannelFuture f = bootstrap.bind(port).sync();

      // Wait until the server socket is closed.
      // In this example, this does not happen, but you can do that to gracefully
      // shut down your server.
      f.channel().closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
      mainGroup.shutdownGracefully();
    }
  }
}
