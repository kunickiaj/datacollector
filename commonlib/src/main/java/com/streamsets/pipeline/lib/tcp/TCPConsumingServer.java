/*
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.lib.tcp;

import com.google.common.collect.ImmutableList;
import com.streamsets.pipeline.api.impl.Utils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class TCPConsumingServer {
  private static final Logger LOG = LoggerFactory.getLogger(TCPConsumingServer.class);
  private static final String NETTY_UNSAFE = "io.netty.noUnsafe";
  private final int numThreads;
  private final List<InetSocketAddress> addresses;
  private final List<ChannelFuture> channelFutures = new ArrayList<>();
  private final List<EventLoopGroup> groups = new ArrayList<>();
  private final TCPConsumer tcpConsumer;

  public TCPConsumingServer(int numThreads, List<InetSocketAddress> addresses, TCPConsumer tcpConsumer) {
    this.numThreads = numThreads;
    this.addresses = ImmutableList.copyOf(addresses);
    this.tcpConsumer = tcpConsumer;
  }

  public void listen() throws InterruptedException {
    for (SocketAddress address : addresses) {
      ServerBootstrap b = bootstrap();
      LOG.info("Starting server on address {}", address);
      for (int i = 0; i < numThreads; i++) {
        ChannelFuture channelFuture = b.bind(address).sync();
        channelFutures.add(channelFuture);
      }
    }
  }

  private ServerBootstrap bootstrap() {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup(numThreads);
    groups.add(bossGroup);
    groups.add(workerGroup);

    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new TCPConsumingServerHandler(tcpConsumer));
          }
        })
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true);
    return b;
  }

  public void destroy() {
    LOG.info("Destroying server on address(es) {}", addresses);
    for (ChannelFuture channelFuture : channelFutures) {
      if (channelFuture != null && channelFuture.isCancellable()) {
        channelFuture.cancel(true);
      }
    }
    for (EventLoopGroup group : groups) {
      if (group != null && !group.isShutdown() && !group.isShuttingDown()) {
        try {
          group.shutdownGracefully().get();
        } catch (InterruptedException ex) {
          // ignore
        } catch (Exception ex) {
          LOG.error("Unexpected error shutting down: " + ex, ex);
        }
      }
    }
    channelFutures.clear();
  }

  public void start() {
    Utils.checkNotNull(channelFutures, "Channel future cannot be null");
    Utils.checkState(!groups.isEmpty(), "Event group cannot be null");
    for (ChannelFuture channelFuture : channelFutures) {
      channelFuture.channel().closeFuture();
    }
  }

  private static void disableDirectBuffers() {
    // required to fully disable direct buffers which
    // while faster to allocate when shared, come with
    // unpredictable limits
    System.setProperty(NETTY_UNSAFE, "true");
  }
}
