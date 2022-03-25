package com.hbicc.cloud.agent.utils;

public class MyHexUtil {


    private static int getBaseCRC(byte[] bytes) {
        /*
         * ModBus 通信协议的 CRC ( 冗余循环校验码含2个字节, 即 16 位二进制数。 CRC 码由发送设备计算, 放置于所发送信息帧的尾部。 接收信息设备再重新计算所接收信息 (除 CRC
         * 之外的部分）的 CRC, 比较计算得到的 CRC 是否与接收到CRC相符, 如果两者不相符, 则认为数据出错。 1) 预置 1 个 16 位的寄存器为十六进制FFFF(即全为 1) ,
         * 称此寄存器为 CRC寄存器。 2) 把第一个 8 位二进制数据 (通信信息帧的第一个字节) 与 16 位的 CRC寄存器的低 8 位相异或, 把结果放于 CRC寄存器。 3) 把 CRC
         * 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位。 4) 如果移出位为 0, 重复第 3 步 ( 再次右移一位); 如果移出位为 1, CRC 寄存器与多项式A001
         * ( 1010 0000 0000 0001) 进行异或。 5) 重复步骤 3 和步骤 4, 直到右移 8 次,这样整个8位数据全部进行了处理。 6) 重复步骤 2 到步骤 5,
         * 进行通信信息帧下一个字节的处理。 7) 将该通信信息帧所有字节按上述步骤计算完成后,得到的16位CRC寄存器的高、低字节进行交换。 8) 最后得到的 CRC寄存器内容即为 CRC码。
         */
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        return CRC;
    }


    /**
     * 计算CRC16校验码 高位在后
     *
     * @param bytes 数组
     * @return 校验位
     */
    public static String getCRC(byte[] bytes) {
        int CRC = getBaseCRC(bytes);
        // 高低位转换，看情况使用
        CRC = ((CRC & 0x0000FF00) >> 8) | ((CRC & 0x000000FF) << 8);
        return Integer.toHexString(CRC).toLowerCase();
    }


    /**
     * 计算CRC16校验码 高位在前
     *
     * @param bytes 数组
     * @return 校验位
     */
    public static String getCRCLower(byte[] bytes) {
        int CRC = getBaseCRC(bytes);
        return addZeroForNumLeft(Integer.toHexString(CRC).toLowerCase(), 4);
    }

    /**
     * 字符串左侧自动补零
     *
     * @param str
     * @param strLength
     * @return
     */
    public static String addZeroForNumLeft(String str, int strLength) {
        int strLen = str.length();
        if (strLen < strLength) {
            while (strLen < strLength) {
                StringBuffer sb = new StringBuffer();
                sb.append("0").append(str);// 左补0
                // sb.append(str).append("0");//右补0
                str = sb.toString();
                strLen = str.length();
            }
        }
        return str;
    }


}
