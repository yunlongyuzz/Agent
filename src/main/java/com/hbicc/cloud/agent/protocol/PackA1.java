package com.hbicc.cloud.agent.protocol;

import lombok.Data;

/**
 * 特征值上报数据包
 */
@Data
public class PackA1 {
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
     * X加速度
     */
    private Double accelerationX;
    /**
     * Y加速度
     */
    private Double accelerationY;
    /**
     * Z加速度
     */
    private Double accelerationZ;

    /**
     * X速度
     */
    private Double speedX;
    /**
     * Y速度
     */
    private Double speedY;
    /**
     * Z速度
     */
    private Double speedZ;

    /**
     * X位移
     */
    private Double displacementX;
    /**
     * Y位移
     */
    private Double displacementY;
    /**
     * Z位移
     */
    private Double displacementZ;

    /**
     * 温度
     */
    private Double temp;

    /**
     * 采样率
     */
    private int frequency;

    /**
     * 电压
     */
    private Double voltage;

    /**
     * 休眠时间
     */
    private int sleepTime;

    /**
     * 声强 分贝值
     */
    private Double voice;

}
