package com.hbicc.cloud.agent.protocol;

import java.util.List;

import lombok.Data;

/**
 * 音频时域波形
 */
@Data
public class PackB3 {
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
   * 分包号
   */
  private int packIndex;
  /**
   * 总包数
   */
  private int packCount;
  /**
   * 加速度数据
   */
  private List<Double> voices;
}
