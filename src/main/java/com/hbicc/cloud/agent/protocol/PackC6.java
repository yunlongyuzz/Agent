package com.hbicc.cloud.agent.protocol;

import lombok.Data;

/**
 * 系统参数数据包
 */
@Data
public class PackC6 {
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
     * 硬件版本
     */
    private String hardwareVersion;

    /**
     * 软件版本
     */
    private String softwareVersion;

    /**
     * 采样率
     */
    private int frequency;

    /**
     * 休眠时间
     */
    private int sleepTime;

    /**
     * 设备编号
     */
    private String reDeviceId;
}
