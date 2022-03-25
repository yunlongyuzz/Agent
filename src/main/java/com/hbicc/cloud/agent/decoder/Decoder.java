package com.hbicc.cloud.agent.decoder;

import java.util.List;
import cn.hutool.core.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
// import lombok.extern.slf4j.Slf4j;

// @Slf4j
public class Decoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {

        // ByteBuf tmp = in.duplicate();
        // log.info("所有字节:{}",
        // HexUtil.encodeHexStr(tmp.readBytes(tmp.readableBytes()).array()).toUpperCase());

        ByteBuf result;
        do {
            result = find(in);
            if ((result != null)) {
                out.add(result);
            }
        } while (result != null);
    }




    private ByteBuf find(ByteBuf buf) {
        try {
            return findV1(buf);
            // int index = buf.readerIndex();
            // byte tmp = buf.getByte(index);
            // if (tmp == 0x7E) {
            //     return findV1(buf);
            // }else if (tmp== 0xFA - 256 | tmp == 0xCA - 256 | tmp == 0xCB - 256 | tmp == 0xCC - 256 | tmp == 0xCD - 256 | tmp == 0xCE - 256 | tmp == 0xCF - 256) {
            //     return findV2(buf);
            // }else {
            //     return null;
            // }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查找包
     *
     * @param buf
     * @return
     */
    private ByteBuf findV1(ByteBuf buf) {
        int count = buf.readableBytes();
        int index = buf.readerIndex();
        if(count < 2){
            return null;
        }

        boolean startFound = false;
        for (int i = index; i < index + count - 1; i++) {
            if (buf.getByte(i) == 0x7E & buf.getByte(i + 1) == 0x7E) {
                startFound = true;
                buf.readerIndex(i);
                break;
            }
        }
        if (!startFound)
            return null;

        count = buf.readableBytes();
        index = buf.readerIndex();
        if (count < 7) {
            return null;
        }
        boolean endFound = false;
        byte[] lens = new byte[2];
        lens[0] = buf.getByte(index + 6);
        lens[1] = buf.getByte(index + 7);
        int len = 0;
        try {
            len = Integer.parseInt(HexUtil.encodeHexStr(lens));
            if (count < 12 + len) {
                return null;
            }
            if (buf.getByte(10 + index + len) == 0x7F & buf.getByte(11 + index + len) == 0x7F) {
                endFound = true;
            }
        } catch (Exception e) {
            endFound = false;
        }

        if (endFound) {
            ByteBuf result = buf.readBytes(12 + len);
            return result;
        }else{
            // 遭遇错包....跳过当前包
            for (int i = index + 2; i < index + count - 1; i++) {
                if (buf.getByte(i) == 0x7E & buf.getByte(i + 1) == 0x7E) {
                    buf.readerIndex(i);
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 查找包
     *
     * @param buf
     * @return
     */
    private ByteBuf findV2(ByteBuf buf) {
        int count = buf.readableBytes();
        int index = buf.readerIndex();

        boolean startFound = false;
        for (int i = index; i < index + count - 1; i++) {
            Integer tmp = buf.getByte(i) & 0xff;
            if (tmp == 0xFA | tmp == 0xCA | tmp == 0xCB | tmp == 0xCC | tmp == 0xCD
                    | tmp == 0xCE | tmp == 0xCF) {
                startFound = true;
                buf.readerIndex(i);
                break;
            }
        }
        if (!startFound)
            return null;

        count = buf.readableBytes();
        index = buf.readerIndex();

        // boolean endFound = false;
        int len = 0;
        int headerLen = 0;
        if (count < 3)
            return null;
        Integer len1 = buf.getByte(index + 3) & 0xFF;
        if (len1 > 127) {
            if (count < 4)
                return null;
            Integer len2 = buf.getByte(index + 4) & 0xFF;
            if (len2 > 127) {
                if (count < 5)
                    return null;
                Integer len3 = buf.getByte(index + 5) & 0xFF;
                if (len3 > 127) {
                    if (count < 6)
                        return null;
                    Integer len4 = buf.getByte(index + 6) & 0xFF;
                    if (len4 > 127) {
                        // 长度不符合
                        return null;
                    } else {
                        len = (len1 - 128) * 128 * 128 * 128 + (len2 - 128) * 128 * 128 + (len3 - 128) * 128 + len4;
                        headerLen = 7;
                    }
                } else {
                    len = (len1 - 128) * 128 * 128 + (len2 - 128) * 128 + len3;
                    headerLen = 6;
                }
            } else {
                len = (len1 - 128) * 128 + len2;
                headerLen = 5;
            }
        } else {
            len = len1;
            headerLen = 4;
        }

        if (count < headerLen + len) {
            return null;
        }

        if (len > 0 & headerLen > 0) {
            ByteBuf result = buf.readBytes(headerLen + len);
            return result;
        }
        return null;
    }
}
