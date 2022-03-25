package com.hbicc.cloud.agent.protocol.v1;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.HexUtil;
// import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.hbicc.cloud.agent.handler.TcpServerHandler;
import com.hbicc.cloud.agent.output.MyKafkaClient;
import com.hbicc.cloud.agent.output.MyMqttClient;
import com.hbicc.cloud.agent.protocol.Acceleration;
import com.hbicc.cloud.agent.protocol.Device;
import com.hbicc.cloud.agent.protocol.Pack;
import com.hbicc.cloud.agent.protocol.PackA1;
import com.hbicc.cloud.agent.protocol.PackA2;
import com.hbicc.cloud.agent.protocol.PackB1;
import com.hbicc.cloud.agent.protocol.PackB3;
import com.hbicc.cloud.agent.protocol.PackC5;
import com.hbicc.cloud.agent.protocol.PackC6;
import com.hbicc.cloud.agent.protocol.PackC8;
import com.hbicc.cloud.agent.utils.MyHexUtil;
import com.hbicc.cloud.agent.utils.WriterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ParserV1 {

    @Value("${company.id}")
    private String companyId;  //COM0000000I

    @Value("${spring.output.type}")
    private String outputType;  //none

    private static String packBegin = "7E7E";
    private static String packEnd = "7F7F";
    private static String now;

    // 此处演示代码, 简单实现, 用 ConcurrentHashMap 存储一些临时变量的数据, 生产环境, 请使用别的方式处理.
    // Device List
    private static ConcurrentHashMap<String, Device> deviceList;

    @PostConstruct
    public void init() {
        deviceList = MapUtil.newConcurrentHashMap();
    }

    //给全局变量 now 赋值
    public void buildNow() {
        now = DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss");
    }


  //*****************************************************************
    //解析各种包，然后存入到日志


    /**
     * 解析数据包
     * @param clientId
     * @param in
     * @return
     */
    public boolean parseData(String clientId, String in) {
        try {
            int inLen = in.length();

            if (inLen < 16) {

                log.info("包长太小!");
                WriterUtil.WriterIO("包长太小!");


                return false;
            }

            if (!packBegin.equals(in.substring(0, 4))) {
                log.info("包头不正确!");
                WriterUtil.WriterIO("包头不正确!");
                return false;
            }

            int dataLen = 0;
            dataLen = Integer.parseInt(in.substring(12, 16));

            if (inLen < 24 + dataLen * 2) {
                log.info("包长不正确!");
                WriterUtil.WriterIO("包长不正确!");
                return false;
            }

            String crc = MyHexUtil.getCRCLower(HexUtil.decodeHex(in.substring(0, 16 + dataLen * 2)));
            crc = MyHexUtil.addZeroForNumLeft(crc, 4);
            if (!crc.equals(in.substring(16 + dataLen * 2, 20 + dataLen * 2).toLowerCase())) {
                log.info("CRC校验不正确!");
                WriterUtil.WriterIO("CRC校验不正确!");
                return false;
            }

            if (!packEnd.equals(in.substring(20 + dataLen * 2, 24 + dataLen * 2))) {
                log.info("包尾不对!");
                WriterUtil.WriterIO("包尾不对!");
                return false;
            }

            buildNow();

            String deviceId = in.substring(4, 8);
            String funcCode = in.substring(8, 10);
            String frameType = in.substring(10, 12);

            if (deviceList.containsKey(deviceId)) {
                deviceList.get(deviceId).setClientId(clientId);
            } else {
                Device tmp = new Device();
                tmp.setClientId(clientId);
                tmp.setFeatureCount(0);
                tmp.setWave1Count(0);
                tmp.setWave2Count(0);
                tmp.setWave3Count(0);
                tmp.setSensorType(0);
                tmp.setLastFeatureTime((long) 0);
                tmp.setLastWaveTime((long) 0);
                deviceList.put(deviceId, tmp);
            }

            if ("00".equals(funcCode) && "00".equals(frameType)) {
                // 初始化
                int idx = TcpServerHandler.addDevice(clientId, deviceId);
                deviceList.get(deviceId).setSensorType(Integer.parseInt(in.substring(14, 16)));

                doPack00(clientId, in, deviceId, funcCode, frameType, idx);
            } else if ("B1".equals(funcCode) && "20".equals(frameType)) {
                // 加速度录波
                deviceList.get(deviceId).setWave1Count(deviceList.get(deviceId).getWave1Count() + 1);
                TcpServerHandler.setChannelIsGettingWave(clientId, deviceId, "run");
                doPackB1(clientId, in, deviceId, funcCode, frameType);
            } else if ("B1".equals(funcCode) && "50".equals(frameType)) {
                // 加速度录波 大包
                doPackB150(clientId, in, deviceId, funcCode, frameType);
            } else if ("B1".equals(funcCode) && "06".equals(frameType)) {
                // 询问波形是否传输完毕.
                isWaveComplete(clientId, in, deviceId, funcCode, frameType);
            } else if ("B3".equals(funcCode) && "20".equals(frameType)) {
                // 音频录波
                deviceList.get(deviceId).setWave2Count(deviceList.get(deviceId).getWave2Count() + 1);
                TcpServerHandler.setChannelIsGettingWave(clientId, deviceId, "run");
                doPackB3(clientId, in, deviceId, funcCode, frameType);
            } else if ("A1".equals(funcCode) && "03".equals(frameType)) {
                // 主动上报数据包
                deviceList.get(deviceId).setFeatureCount(deviceList.get(deviceId).getFeatureCount() + 1);
                TcpServerHandler.setChannelIsGettingWave(clientId, deviceId, "end");
                ParserV1.getDeviceClientList().get(deviceId).setLastFeatureTime(System.currentTimeMillis());
                TcpServerHandler.getChannelList().get(clientId).getFeatureAckLisk().addFirst(deviceId);
                doPackA1(clientId, in, deviceId, funcCode, frameType);
            } else if ("A2".equals(funcCode) && "03".equals(frameType)) {
                // 电流 上报 数据包
                deviceList.get(deviceId).setFeatureCount(deviceList.get(deviceId).getFeatureCount() + 1);
                TcpServerHandler.setChannelIsGettingWave(clientId, deviceId, "end");
                ParserV1.getDeviceClientList().get(deviceId).setLastFeatureTime(System.currentTimeMillis());
                TcpServerHandler.getChannelList().get(clientId).getFeatureAckLisk().addFirst(deviceId);
                doPackA2(clientId, in, deviceId, funcCode, frameType);
            } else if ("D1".equals(funcCode) && "03".equals(frameType)) {
                // 心跳包
                doPackD1(clientId, in, deviceId, funcCode, frameType);
            } else if ("C1".equals(funcCode) && "10".equals(frameType)) {
                // 设置采样率 回传数据包
                doPackC1(clientId, in, deviceId, funcCode, frameType);
            } else if ("C2".equals(funcCode) && "10".equals(frameType)) {
                // 设置休眠时间 回传数据包
                doPackC2(clientId, in, deviceId, funcCode, frameType);
            } else if ("C3".equals(funcCode) && "10".equals(frameType)) {
                // 设置设备ID 回传数据包
                doPackC3(clientId, in, deviceId, funcCode, frameType);
            } else if ("C4".equals(funcCode) && "10".equals(frameType)) {
                // 设置设备ID 回传数据包
                doPackC4(clientId, in, deviceId, funcCode, frameType);
            } else if ("C5".equals(funcCode) && "10".equals(frameType)) {
                // 获取lora参数 回传数据包
                doPackC5(clientId, in, deviceId, funcCode, frameType);
            } else if ("C6".equals(funcCode) && "10".equals(frameType)) {
                // 获取系统参数 回传数据包
                doPackC6(clientId, in, deviceId, funcCode, frameType);
            } else if ("C7".equals(funcCode) && "10".equals(frameType)) {
                // 这只WiFi参数 回传数据包
                doPackC7(clientId, in, deviceId, funcCode, frameType);
            } else if ("C8".equals(funcCode) && "10".equals(frameType)) {
                // 获取WIFI参数 回传数据包
                doPackC8(clientId, in, deviceId, funcCode, frameType);
            }
            return true;
        } catch (Exception e) {
            log.info("解析发生错误: {}", e.getMessage());
            WriterUtil.WriterIO("解析发生错误: {}", e.getMessage());
            return false;
        }
    }


    // 终端上电后, 第一个数据包, 返回服务器时间和序号
    public void doPack00(String clientId, String in, String deviceId, String funcCode, String frameType, int idx) {
        String dataStr = "";
        DateTime now = DateTime.now();
        byte[] time = {0, 0, 0, 0, 0, 0, 0};
        time[0] = (byte) (now.year() - 2000);
        time[1] = (byte) now.month();
        time[2] = (byte) now.dayOfMonth();
        time[3] = (byte) now.hour(true);
        time[4] = (byte) now.minute();
        time[5] = (byte) now.second();
        time[6] = (byte) idx;
        dataStr = HexUtil.encodeHexStr(time);
        Console.log("dateStart: {}", dataStr);
        String cmd = BuildPack(deviceId, "00", "00", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 主动上报数据 A1数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackA1(String clientId, String in, String deviceId, String funcCode, String frameType) {
        DecimalFormat df = new DecimalFormat("##0.00");

        PackA1 packA1 = new PackA1();
        packA1.setDeviceId(deviceId);
        packA1.setFuncCode(funcCode);
        packA1.setFrameType(frameType);

        packA1.setAccelerationX(Double.valueOf(df.format(Double.valueOf(in.substring(16, 20)) / 100)));
        packA1.setAccelerationY(Double.valueOf(df.format(Double.valueOf(in.substring(20, 24)) / 100)));
        packA1.setAccelerationZ(Double.valueOf(df.format(Double.valueOf(in.substring(24, 28)) / 100)));
        packA1.setSpeedX(Double.valueOf(df.format(Double.valueOf(in.substring(28, 32)) / 100)));
        packA1.setSpeedY(Double.valueOf(df.format(Double.valueOf(in.substring(32, 36)) / 100)));
        packA1.setSpeedZ(Double.valueOf(df.format(Double.valueOf(in.substring(36, 40)) / 100)));
        packA1.setDisplacementX(Double.valueOf(Integer.parseInt(in.substring(40, 44))));
        packA1.setDisplacementY(Double.valueOf(Integer.parseInt(in.substring(44, 48))));
        packA1.setDisplacementZ(Double.valueOf(Integer.parseInt(in.substring(48, 52))));
        packA1.setVoice(Double.valueOf(Integer.parseInt(in.substring(52, 58))));
        packA1.setTemp(Double.valueOf(in.substring(58, 62)) / 10);
        packA1.setVoltage(Double.valueOf(in.substring(62, 66)) / 100);
        packA1.setSleepTime(Integer.parseInt(in.substring(66, 74)));
        packA1.setFrequency(Integer.parseInt(in.substring(74, 82)));

        packA1.setClientId(clientId);

        // String ackString = Ack(deviceId, funcCode, frameType);
        // TcpServerHandler.sendMessage(clientId, ackString);

        String jsonString = JSONUtil.toJsonStr(packA1);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }

        oneDayGetAccelerationWave(deviceId, clientId);
    }

    /**
     * 电流上报特征值 A2数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackA2(String clientId, String in, String deviceId, String funcCode, String frameType) {

        DecimalFormat df = new DecimalFormat("##0.00");

        PackA2 packA2 = new PackA2();
        packA2.setDeviceId(deviceId);
        packA2.setFuncCode(funcCode);
        packA2.setFrameType(frameType);

        packA2.setAmpereA(Double.valueOf(df.format(Double.valueOf(in.substring(16, 20)) / 100)));
        packA2.setAmpereB(Double.valueOf(df.format(Double.valueOf(in.substring(20, 24)) / 100)));
        packA2.setAmpereC(Double.valueOf(df.format(Double.valueOf(in.substring(24, 28)) / 100)));
        packA2.setVoltage(Double.valueOf(in.substring(28, 32)) / 100);
        packA2.setSleepTime(Integer.parseInt(in.substring(32, 40)));
        packA2.setFrequency(Integer.parseInt(in.substring(40, 48)));

        packA2.setClientId(clientId);

        // String ackString = Ack(deviceId, funcCode, frameType);
        // TcpServerHandler.sendMessage(clientId, ackString);

        String jsonString = JSONUtil.toJsonStr(packA2);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }

        oneDayGetAccelerationWave(deviceId, clientId);
    }

    /**
     * 心跳包回复
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackD1(String clientId, String in, String deviceId, String funcCode, String frameType) {
        Pack pack = new Pack();
        pack.setDeviceId(deviceId);
        pack.setFuncCode(funcCode);
        pack.setFrameType(frameType);

        pack.setClientId(clientId);

        String ackString = Ack(deviceId, funcCode, frameType);
        TcpServerHandler.sendMessage(clientId, ackString);

        String jsonString = JSONUtil.toJsonStr(pack);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
    }

    /**
     * 设置采样率 回传数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC1(String clientId, String in, String deviceId, String funcCode, String frameType) {
        Pack pack = new Pack();
        pack.setDeviceId(deviceId);
        pack.setFuncCode(funcCode);
        pack.setFrameType(frameType);

        pack.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(pack);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("设置采样率成功！");
        WriterUtil.WriterIO("设置采样率成功！");
    }

    /**
     * 设置休眠时间 回传数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC2(String clientId, String in, String deviceId, String funcCode, String frameType) {
        Pack pack = new Pack();
        pack.setDeviceId(deviceId);
        pack.setFuncCode(funcCode);
        pack.setFrameType(frameType);

        pack.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(pack);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("设置休眠时间成功！");
        WriterUtil.WriterIO("设置休眠时间成功！");
    }

    /**
     * 设置设备ID 回传数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC3(String clientId, String in, String deviceId, String funcCode, String frameType) {
        Pack pack = new Pack();
        pack.setDeviceId(deviceId);
        pack.setFuncCode(funcCode);
        pack.setFrameType(frameType);

        pack.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(pack);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("设置设备ID成功！");
        WriterUtil.WriterIO("设置设备ID成功！");
    }

    /**
     * 设置lora参数 返回
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC4(String clientId, String in, String deviceId, String funcCode, String frameType) {
        Pack pack = new Pack();
        pack.setDeviceId(deviceId);
        pack.setFuncCode(funcCode);
        pack.setFrameType(frameType);

        pack.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(pack);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("设置LORA参数成功！");
        WriterUtil.WriterIO("设置LORA参数成功！");
    }

    /**
     * 获取lora参数 回传数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC5(String clientId, String in, String deviceId, String funcCode, String frameType) {
        log.info("获取lora参数成功！");
        WriterUtil.WriterIO("获取lora参数成功！");
        DecimalFormat df = new DecimalFormat("##0.00");

        PackC5 packC5 = new PackC5();
        packC5.setDeviceId(deviceId);
        packC5.setFuncCode(funcCode);
        packC5.setFrameType(frameType);

        packC5.setSendFrequency(Double.valueOf(df.format(Double.valueOf(in.substring(16, 20)) / 10)));
        packC5.setRecvFrequency(Double.valueOf(df.format(Double.valueOf(in.substring(20, 24)) / 10)));
        packC5.setSendFactor(Integer.parseInt(in.substring(24, 26)));
        packC5.setRecvFactor(Integer.parseInt(in.substring(26, 28)));
        packC5.setPreCode(Integer.parseInt(in.substring(28, 32)));

        packC5.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(packC5);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
    }

    /**
     * 获取系统参数 回传数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC6(String clientId, String in, String deviceId, String funcCode, String frameType) {
        log.info("获取系统参数成功！");
        WriterUtil.WriterIO("获取系统参数成功！");
        // DecimalFormat df = new DecimalFormat("##0.00");

        PackC6 packC6 = new PackC6();
        packC6.setDeviceId(deviceId);
        packC6.setFuncCode(funcCode);
        packC6.setFrameType(frameType);

        packC6.setHardwareVersion(in.substring(16, 20));
        packC6.setHardwareVersion(in.substring(20, 24));
        packC6.setFrequency(Integer.parseInt(in.substring(24, 32)));
        packC6.setSleepTime(Integer.parseInt(in.substring(32, 40)));
        packC6.setReDeviceId(in.substring(40, 44));

        packC6.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(packC6);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
    }

    /**
     * 设置WIFI参数
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC7(String clientId, String in, String deviceId, String funcCode, String frameType) {
        Pack pack = new Pack();
        pack.setDeviceId(deviceId);
        pack.setFuncCode(funcCode);
        pack.setFrameType(frameType);

        pack.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(pack);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("设置WIFI参数");
        WriterUtil.WriterIO("设置WIFI参数");
    }

    /**
     * 获取WIFI参数 回传数据包
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackC8(String clientId, String in, String deviceId, String funcCode, String frameType) {
        log.info("获取WIFI参数");
        WriterUtil.WriterIO("获取WIFI参数");
        PackC8 packC8 = new PackC8();
        packC8.setDeviceId(deviceId);
        packC8.setFuncCode(funcCode);
        packC8.setFrameType(frameType);

        Integer len1 = Integer.parseInt(in.substring(16, 18)) * 2;
        Integer len2 = Integer.parseInt(in.substring(18, 20)) * 2;
        Integer len3 = Integer.parseInt(in.substring(20, 22)) * 2;
        Integer len4 = Integer.parseInt(in.substring(22, 24)) * 2;

        packC8.setServerIp(new String(HexUtil.decodeHex(in.substring(24, 24 + len1))));
        packC8.setServerPort(new String(HexUtil.decodeHex(in.substring(24 + len1, 24 + len1 + len2))));
        packC8.setWifiSsid(new String(HexUtil.decodeHex(in.substring(24 + len1 + len2, 24 + len1 + len2 + len3))));
        packC8.setWifiPwd(new String(HexUtil.decodeHex(in.substring(24 + len1 + len2 + len3, 24 + len1 + len2 + len3 + len4))));

        packC8.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(packC8);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
    }

    /**
     * 获取加速度波形
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackB1(String clientId, String in, String deviceId, String funcCode, String frameType) {
        // DecimalFormat df = new DecimalFormat("##0.00");

        PackB1 packB1 = new PackB1();
        packB1.setDeviceId(deviceId);
        packB1.setFuncCode(funcCode);
        packB1.setFrameType(frameType);

        packB1.setPackIndex(Integer.parseInt(in.substring(16, 20)));
        packB1.setPackCount(Integer.parseInt(in.substring(20, 24)));

        deviceList.get(deviceId).getWave1PackIndex().add(packB1.getPackIndex());

        List<Acceleration> accelerations = new ArrayList<Acceleration>();

        for (int i = 0; i < 10; i++) {
            Acceleration tmp = new Acceleration();
            int offset = i * 12;
            // tmp.setAccelerationX(
            // Double.valueOf(df.format(Double.valueOf(in.substring(24 + offset, 28 +
            // offset)) / 100)));
            // tmp.setAccelerationY(
            // Double.valueOf(df.format(Double.valueOf(in.substring(28 + offset, 32 +
            // offset)) / 100)));
            // tmp.setAccelerationZ(
            // Double.valueOf(df.format(Double.valueOf(in.substring(32 + offset, 36 +
            // offset)) / 100)));
            // accelerations.add(tmp);
            tmp.setAccelerationX(Double.valueOf(Integer.parseInt(in.substring(24 + offset, 28 + offset), 16)));
            tmp.setAccelerationY(Double.valueOf(Integer.parseInt(in.substring(28 + offset, 32 + offset), 16)));
            tmp.setAccelerationZ(Double.valueOf(Integer.parseInt(in.substring(32 + offset, 36 + offset), 16)));
            accelerations.add(tmp);
        }
        packB1.setAccelerations(accelerations);

        packB1.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(packB1);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }

        // log.info("获取加速度波形成功！");
    }

    public void doPackB150(String clientId, String in, String deviceId, String funcCode, String frameType) {
        // DecimalFormat df = new DecimalFormat("##0.00");

        PackB1 packB1 = new PackB1();
        packB1.setDeviceId(deviceId);
        packB1.setFuncCode(funcCode);
        packB1.setFrameType(frameType);

        packB1.setPackIndex(Integer.parseInt(in.substring(12, 16)));
        packB1.setPackCount(Integer.parseInt(in.substring(16, 20)));
        // TODO 实现大包协议

        String jsonString = JSONUtil.toJsonStr(packB1);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("获取加速度 大 包 波形成功！");
        WriterUtil.WriterIO("获取加速度 大 包 波形成功！");
    }

    /**
     * 获取音频波形
     *
     * @param clientId
     * @param in
     * @param deviceId
     * @param funcCode
     * @param frameType
     */
    public void doPackB3(String clientId, String in, String deviceId, String funcCode, String frameType) {
        PackB3 packB3 = new PackB3();
        packB3.setDeviceId(deviceId);
        packB3.setFuncCode(funcCode);
        packB3.setFrameType(frameType);

        packB3.setPackIndex(Integer.parseInt(in.substring(16, 20)));
        packB3.setPackCount(Integer.parseInt(in.substring(20, 24)));

        List<Double> voices = new ArrayList<Double>();

        for (int i = 0; i < 10; i++) {
            int offset = i * 6;
            Double tmp = Double.valueOf(Integer.parseInt(in.substring(24 + offset, 30 + offset)));
            voices.add(tmp);
        }
        packB3.setVoices(voices);

        packB3.setClientId(clientId);

        String jsonString = JSONUtil.toJsonStr(packB3);
        if (outputType.equals("kafka")) {
            MyKafkaClient.pub(jsonString);
        }
        if (outputType.equals("mqtt")) {
            MyMqttClient.pub(jsonString);
        }
        log.info("获取音频波形成功！");
        WriterUtil.WriterIO("获取音频波形成功！");
    }



    //**********************************************************
    //在解析包中需要用到的一系列操作，然后返回tcp类中的sendMessage


    /**
     * 获取系统参数
     *
     * @param deviceId
     */
    public void getSysParam(String deviceId) {
        String dataStr = "";
        String cmd = BuildPack(deviceId, "C6", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 设置 采样率
     *
     * @param deviceId
     * @param frequency
     */
    public void setFrequency(String deviceId, int frequency) {
        String dataStr = MyHexUtil.addZeroForNumLeft(Integer.toString(frequency), 8).toUpperCase();
        String cmd = BuildPack(deviceId, "C1", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 设置 休眠时间
     *
     * @param deviceId
     * @param time
     */
    public void setSleepTime(String deviceId, int time) {
        String dataStr = MyHexUtil.addZeroForNumLeft(Integer.toString(time), 8).toUpperCase();
        String cmd = BuildPack(deviceId, "C2", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 设置 设备地址
     * @param deviceId
     * @param newDeviceId
     */
    public void setDeviceId(String deviceId, String newDeviceId) {
        String dataStr = MyHexUtil.addZeroForNumLeft(newDeviceId, 4).toUpperCase();
        String cmd = BuildPack(deviceId, "C3", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 获取lora参数
     *
     * @param deviceId
     */
    public void getLoraParam(String deviceId) {
        String dataStr = "";
        String cmd = BuildPack(deviceId, "C5", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 设置lora参数
     * @param deviceId
     * @param sendFrequency
     * @param recvFrequency
     * @param sendFactor
     * @param recvFactor
     * @param preCode
     */
    public void setLoraParam(String deviceId, Double sendFrequency, Double recvFrequency, Integer sendFactor,
            Integer recvFactor, Integer preCode) {
        DecimalFormat df = new DecimalFormat("0");
        StringBuilder tmp = new StringBuilder();
        tmp.append(MyHexUtil.addZeroForNumLeft(df.format(sendFrequency * 10), 4))
                .append(MyHexUtil.addZeroForNumLeft(df.format(recvFrequency * 10), 4))
                .append(MyHexUtil.addZeroForNumLeft(df.format(sendFactor), 2))
                .append(MyHexUtil.addZeroForNumLeft(df.format(recvFactor), 2))
                .append(MyHexUtil.addZeroForNumLeft(df.format(preCode), 4));

        String dataStr = tmp.toString();
        String cmd = BuildPack(deviceId, "C4", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 获取WIFI参数
     *
     * @param deviceId
     */
    public void getWifiParam(String deviceId) {
        String dataStr = "";
        String cmd = BuildPack(deviceId, "C8", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 设置WiFi参数
     *
     * @param deviceId
     * @param serverIp
     * @param serverPort
     * @param wifiSsid
     * @param wifiPwd
     */
    public void setWifiParam(String deviceId, String serverIp, String serverPort, String wifiSsid, String wifiPwd) {
        DecimalFormat df = new DecimalFormat("0");
        StringBuilder tmp = new StringBuilder();
        tmp.append(MyHexUtil.addZeroForNumLeft(df.format(serverIp.length()), 2))
                .append(MyHexUtil.addZeroForNumLeft(df.format(serverPort.length()), 2))
                .append(MyHexUtil.addZeroForNumLeft(df.format(wifiSsid.length()), 2))
                .append(MyHexUtil.addZeroForNumLeft(df.format(wifiPwd.length()), 2))

                .append(HexUtil.encodeHexStr(serverIp.getBytes())).append(HexUtil.encodeHexStr(serverPort.getBytes()))
                .append(HexUtil.encodeHexStr(wifiSsid.getBytes())).append(HexUtil.encodeHexStr(wifiPwd.getBytes()));

        String dataStr = tmp.toString();
        String cmd = BuildPack(deviceId, "C7", "01", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 获取加速度波形
     *
     * @param deviceId
     */
    public void getAccelerationWave(String deviceId) {
        String dataStr = "";
        String cmd = BuildPack(deviceId, "B1", "02", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 获取加速度波形(大包)
     *
     * @param deviceId
     */
    public void getBigAccelerationWave(String deviceId) {
        String dataStr = "";
        String cmd = BuildPack(deviceId, "B1", "05", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 补发加速度波形,询问包反馈
     *
     * @param deviceId
     */
    public void isWaveComplete(String clientId, String in, String deviceId, String funcCode, String frameType) {
        List<Integer> tmp = new ArrayList<Integer>();
        if (deviceList.get(deviceId).getWave1PackIndex().size() == 200) {
            // 波形收满
            TcpServerHandler.setChannelIsGettingWave(clientId, deviceId, "end");
            deviceList.get(deviceId).getWave1PackIndex().clear();
        } else {
            // 波形没有收满
            // 计算哪些没有收满
            for (int i = 0; i < 200; i++) {
                if (!deviceList.get(deviceId).getWave1PackIndex().contains(i + 1)) {
                    tmp.add(i + 1);
                    if (tmp.size() > 9) {
                        break;
                    }
                }
            }
        }
        String dataStr = "";
        byte[] data;
        int tmpSize = tmp.size();
        data = new byte[tmpSize];
        for (int i = 0; i < tmpSize; i++) {
            data[i] = (byte) tmp.get(i).intValue();
        }
        dataStr = HexUtil.encodeHexStr(data);
        String cmd = BuildPack(deviceId, "B1", "60", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 获取音频波形
     *
     * @param deviceId
     */
    public void getVoiceWave(String deviceId) {
        String dataStr = "";
        String cmd = BuildPack(deviceId, "B3", "02", dataStr);
        String id = deviceList.get(deviceId).getClientId();
        TcpServerHandler.sendMessage(id, cmd);
    }

    /**
     * 接收数据 的 回复内容
     *
     * @param deviceId
     * @param funcCode
     * @param frameType
     * @return
     */
    public String Ack(String deviceId, String funcCode, String frameType) {
        String loraId = MyHexUtil.addZeroForNumLeft(deviceId, 4).toUpperCase();
        StringBuilder ack = new StringBuilder();
        byte[] bs = frameType.getBytes();
        byte[] bs2 = {bs[1], bs[0]};
        String reFrameType = new String(bs2);
        // 原协议里有包序号和包长度, 去掉
        // ack.append(packBegin).append(loraId).append(funcCode).append(reFrameType).append("000000000000");
        ack.append(packBegin).append(loraId).append(funcCode).append(reFrameType).append("0000");

        String crc = MyHexUtil.getCRCLower(HexUtil.decodeHex(ack.toString()));
        ack.append(crc).append(packEnd);
        return ack.toString();
    }

    /**
     * 构建回复包
     *
     * @param deviceId
     * @param funcCode
     * @param frameType
     * @param packData
     * @return
     */
    public static String BuildPack(String deviceId, String funcCode, String frameType, String packData) {
        DecimalFormat df = new DecimalFormat("0");
        StringBuilder cmd = new StringBuilder();
        cmd.append(packBegin).append(deviceId).append(funcCode).append(frameType);
        // 原来有包序号和总包数, 去掉
        // String curPackIndex = MyHexUtil.addZeroForNumLeft("0", 4);
        // String allPackCount = MyHexUtil.addZeroForNumLeft("0", 4);
        // String dataLen = MyHexUtil.addZeroForNumLeft(df.format(packData.length() / 2), 4);
        // cmd.append(curPackIndex).append(allPackCount).append(dataLen).append(packData);
        String dataLen = MyHexUtil.addZeroForNumLeft(df.format(packData.length() / 2), 4);
        cmd.append(dataLen).append(packData);
        String crc = MyHexUtil.getCRCLower(HexUtil.decodeHex(cmd.toString()));
        cmd.append(crc).append(packEnd);
        return cmd.toString();
    }

    /**
     * 每次获取特征值之后, 尝试有必要的话, 获取加速度波形。
     *
     * @param deviceId
     * @throws InterruptedException
     */
    public void oneDayGetAccelerationWave(String deviceId, String clientId) {
        // if (!deviceId.equals("0001")) {
        // return;
        // }

        if (TcpServerHandler.getChannelIsGettingWave(clientId)) {
            // log.info("tcp 通道正在 传波形!!!");
            return;
        }
        // 当前device 是否已经收够波形?
        if (ParserV1.getDeviceClientList().get(deviceId).getWave1Count() < 1) {
            TcpServerHandler.setChannelIsGettingWave(clientId, deviceId, "start");
            ParserV1.getDeviceClientList().get(deviceId).getWave1PackIndex().clear();
            ThreadUtil.execAsync(() -> {
                // 停留0.8秒
                ThreadUtil.sleep(800);
                ParserV1.getDeviceClientList().get(deviceId).setLastWaveTime(System.currentTimeMillis());
                // 获取加速度波形
                getAccelerationWave(deviceId);
            });
        } else {
            return;
        }
    }

    public static Map<String, Device> getDeviceClientList() {
        return deviceList;
    }

    public static Boolean setDeviceClientWave1Count(String deviceId, Integer wave1Count) {
        deviceList.get(deviceId).setWave1Count(wave1Count);
        return true;
    }

    public static Integer getDeviceClientFeatureCount() {
        Integer c = 0;
        for (String key : deviceList.keySet()) {
            c = c + deviceList.get(key).getFeatureCount();
        }
        return c;
    }

    public static Integer getDeviceClientWave1Count() {
        Integer c = 0;
        for (String key : deviceList.keySet()) {
            c = c + deviceList.get(key).getWave1Count();
        }
        return c;
    }
}
