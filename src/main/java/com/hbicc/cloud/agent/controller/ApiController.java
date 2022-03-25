package com.hbicc.cloud.agent.controller;

import com.hbicc.cloud.agent.handler.TcpServerHandler;
import com.hbicc.cloud.agent.protocol.v1.ParserV1;
import com.hbicc.cloud.agent.utils.ResponseUtil;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    @GetMapping("/get_device_list")
    public ResponseUtil getDeviceClientList() {
        return ResponseUtil.out(ParserV1.getDeviceClientList());
    }

    @GetMapping("/set_device_wave1_count")
    public ResponseUtil setDeviceClientWave1Count(@RequestParam String deviceId, @RequestParam Integer c) {
        return ResponseUtil.out(ParserV1.setDeviceClientWave1Count(deviceId, c));
    }

    @GetMapping("/get_device_feature_count")
    public ResponseUtil getDeviceClientA1Count() {
        return ResponseUtil.out(ParserV1.getDeviceClientFeatureCount());
    }

    @GetMapping("/get_device_wave1_count")
    public ResponseUtil getDeviceClientWave1Count() {
        return ResponseUtil.out(ParserV1.getDeviceClientWave1Count());
    }

    @GetMapping("/get_channel_list")
    public ResponseUtil getChannelList() {
        return ResponseUtil.out(TcpServerHandler.getChannelList());
    }
}
