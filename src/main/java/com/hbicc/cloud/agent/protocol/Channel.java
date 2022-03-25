package com.hbicc.cloud.agent.protocol;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

@Data
public class Channel {
    private ChannelHandlerContext ctx;
    private List<String> deviceIds;
    private LinkedList<String> featureAckLisk;
    private Set<String> waveStartDeviceIds;
    private Set<String> waveRunDeviceIds;
}
