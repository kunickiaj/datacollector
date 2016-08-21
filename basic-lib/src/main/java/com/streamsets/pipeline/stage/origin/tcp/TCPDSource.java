package com.streamsets.pipeline.stage.origin.tcp;

import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.Source;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.configurablestage.DSource;
import com.streamsets.pipeline.stage.origin.udp.Groups;

@StageDef(
    version = 1,
    label = "TCP Source",
    description = "Listens for TCP messages on a single port",
    icon = "udp.png",
    execution = ExecutionMode.STANDALONE,
    recordsByRef = true,
    onlineHelpRefUrl = ""
)

@ConfigGroups(Groups.class)
@GenerateResourceBundle
public class TCPDSource extends DSource {
  @Override
  protected Source createSource() {
    return new TCPSource();
  }
}
