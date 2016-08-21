package com.streamsets.pipeline.stage.origin.tcp;

import com.streamsets.pipeline.lib.parser.udp.AbstractParser;
import com.streamsets.pipeline.stage.origin.udp.ParseResult;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class TCPServerHandler extends ChannelInboundHandlerAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(TCPServerHandler.class);

  private final BlockingQueue<ParseResult> records;
  private final AbstractParser parser;

  public TCPServerHandler(final BlockingQueue<ParseResult> records, AbstractParser parser) {
    this.records = records;
    this.parser = parser;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
    try {
      // Do something with msg
      LOG.info("Got message");
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
    // Close the connection when an exception is raised.
    LOG.error(cause.toString(), cause);
    ctx.close();
  }
}
