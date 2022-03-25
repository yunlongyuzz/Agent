package com.hbicc.cloud.agent.task;

import java.util.HashSet;
import java.util.Set;
import com.hbicc.cloud.agent.handler.TcpServerHandler;
import com.hbicc.cloud.agent.protocol.v1.ParserV1;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
// import cn.hutool.core.date.DateTime;
// import lombok.extern.slf4j.Slf4j;
// import cn.hutool.core.util.HexUtil;

// @Slf4j
@Component
public class TimeTask {

    /**
     * 每隔4秒回复特征值 的 已接收ID列表
     */
    @Scheduled(cron = "*/4 * * * * *")
    public void task1() {
        for (String clientId : TcpServerHandler.getChannelList().keySet()) {
            Set<String> sendList = new HashSet<String>();
            int size = TcpServerHandler.getChannelList().get(clientId).getFeatureAckLisk().size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    String deviceId = TcpServerHandler.getChannelList().get(clientId).getFeatureAckLisk().removeLast();
                    sendList.add(deviceId);
                }
                if (sendList.size() > 0) {
                    String dataStr = "";
                    for (String id : sendList) {
                        dataStr = dataStr + id;
                    }
                    String cmd = ParserV1.BuildPack("9999", "A1", "30", dataStr);
                    TcpServerHandler.sendMessage(clientId, cmd);
                }
            }
        }
    }
}
