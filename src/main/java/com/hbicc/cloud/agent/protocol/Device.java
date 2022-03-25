package com.hbicc.cloud.agent.protocol;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Device {
    private String clientId;
    private Integer wave1Count;
    private Integer wave2Count;
    private Integer wave3Count;
    private Integer featureCount;
    private Long lastFeatureTime;
    private Long lastWaveTime;
    private Integer sensorType;
    private List<Integer> wave1PackIndex = new ArrayList<Integer>();
}
