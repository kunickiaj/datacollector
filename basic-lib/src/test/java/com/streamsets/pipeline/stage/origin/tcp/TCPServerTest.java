package com.streamsets.pipeline.stage.origin.tcp;

import com.google.common.base.Charsets;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.lib.parser.udp.syslog.SyslogParser;
import com.streamsets.pipeline.sdk.ContextInfoCreator;
import com.streamsets.pipeline.stage.origin.udp.ParseResult;
import com.streamsets.testing.NetworkUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TCPServerTest {

  @Test
  public void testRun() throws Exception {
    final int port = NetworkUtils.getRandomPort();
    BlockingQueue<ParseResult> queue = new ArrayBlockingQueue<>(10);
    SyslogParser parser = new SyslogParser(getContext(), Charsets.UTF_8);
    TCPServer server = new TCPServer(port, new TCPServerHandler(queue, parser));

  }

  private Stage.Context getContext() {
    return ContextInfoCreator.createSourceContext("i", false, OnRecordError.TO_ERROR,
        Collections.<String>emptyList());
  }
}