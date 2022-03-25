package com.hbicc.cloud.agent.protocol;

import lombok.Data;

/**
 * lora参数数据包
 */
@Data
public class PackC5 {
    /**
     * id
     */
    // private String id;
    /**
     * clientId
     */
    private String clientId;
    /**
     * 数据包原始值
     */
    // private String originalValue;
    /**
     * 设备编号
     */
    private String deviceId;
    /**
     * 硬件功能码
     */
    private String funcCode;
    /**
     * 帧类型：01命令数据帧 10命令数据帧应答 02波形数据帧 20波形数据帧应答 03传感器主动上传 30传感器主动上传应答 04故障录波
     */
    private String frameType;
    /**
     * 发射频率
     */
    private Double sendFrequency;

    /**
     * 接收频率
     */
    private Double recvFrequency;

    /**
     * 发射因子
     */
    private Integer sendFactor;

    /**
     * 接收因子
     */
    private Integer recvFactor;

    /**
     * 前导码
     */
    private Integer preCode;
}
