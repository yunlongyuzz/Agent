package com.hbicc.cloud.agent.protocol;

import lombok.Data;

/**
 * 电流 特征值 数据包
 */
@Data
public class PackA2 {
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
   * A相电流
   */
  private Double ampereA;
  /**
   * B相电流
   */
  private Double ampereB;
  /**
   * C相电流
   */
  private Double ampereC;

  /**
   * 电压
   */
  private Double voltage;

  /**
   * 休眠时间
   */
  private int sleepTime;

  /**
   * 采样率
   */
  private int frequency;

  /**
   * 声强 分贝值
   */
  private Double voice;
}
