package com.hbicc.cloud.agent.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.hbicc.cloud.agent.utils.WriterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import com.hbicc.cloud.agent.protocol.Channel;
import com.hbicc.cloud.agent.protocol.v1.ParserV1;

//连接操作
@Component
@Slf4j
public class TcpServerHandler extends ChannelHandlerAdapter {

    private static TcpServerHandler tcpServerHandler;

    // 此处演示代码, 简单实现, 用 ConcurrentHashMap 存储一些临时变量的数据, 生产环境, 请使用别的方式处理.
    // TCP channel List
    public static ConcurrentHashMap<String, Channel> channelList = MapUtil.newConcurrentHashMap();

    // @Autowired
    // public ParserV2 parserV2;

    @Autowired
    public ParserV1 parserV1;

    @PostConstruct
    public void init() {
        tcpServerHandler = this;
    }

    /**
     * 客户端连接会触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新的连接......");
        WriterUtil.WriterIO("新的连接......");
        Channel tmp = new Channel();
        List<String> deviceIds = new ArrayList<String>();
        Set<String> tmpWaveRunList = new HashSet<String>();
        Set<String> tmpWaveStartList = new HashSet<String>();
        LinkedList<String> featureAckLisk = new LinkedList<String>();

        tmp.setCtx(ctx);
        tmp.setDeviceIds(deviceIds);
        tmp.setWaveStartDeviceIds(tmpWaveStartList);
        tmp.setWaveRunDeviceIds(tmpWaveRunList);
        tmp.setFeatureAckLisk(featureAckLisk);
        channelList.put(ctx.channel().id().asShortText(), tmp);
    }

    /**
     * 客户端发消息会触发
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("==================================");
        WriterUtil.WriterIO("==================================");
        ByteBuf recvByteBuf = (ByteBuf) msg;

        byte[] recvData = new byte[recvByteBuf.readableBytes()];
        recvByteBuf.readBytes(recvData);
        String recvHex = HexUtil.encodeHexStr(recvData).toUpperCase();
        // log.info("recvData: {}", recvData);
        log.info("接收Hex: {}", recvHex);
        WriterUtil.WriterIO("接收Hex: {}", recvHex);
        boolean re;
        // if (recvHex.charAt(0) == '7') {
        re = tcpServerHandler.parserV1.parseData(ctx.channel().id().asShortText(), recvHex);
        // } else {
        // re = tcpServerHandler.parserV2.parseData(ctx.channel().id().asShortText(), recvHex);
        // }
        log.info("解析结果：{}", re);
        WriterUtil.WriterIO("解析结果：{}", re);
        ReferenceCountUtil.release(recvByteBuf);
    }

    /**
     * 发生异常触发
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    /**
     * 客户端主动断开服务端的连接
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("连接断开......");
        WriterUtil.WriterIO("连接断开......");
        removeChannelMap(ctx);
        super.channelInactive(ctx);
    }

    /**
     * 移除已经失效的链接
     *
     * @param ctx
     */
    private void removeChannelMap(ChannelHandlerContext ctx) {
        for (String key : channelList.keySet()) {
            if (channelList.get(key) != null && channelList.get(key).getCtx().equals(ctx)) {
                channelList.remove(key);
            }
        }
    }

    public static void sendMessage(String id, String data) {
        if (id == null || "".equals(id)) {
            return;
        }
        ChannelHandlerContext ctx = channelList.get(id).getCtx();
        if (ctx != null) {
            ThreadUtil.execAsync(() -> {
                // 停留0.03秒
                ThreadUtil.sleep(30);
                // 发送回复
                log.info("服务器发送: {}", data.toUpperCase());
                WriterUtil.WriterIO("服务器发送: {}", data.toUpperCase());
                ByteBuf respByteBuf = Unpooled.copiedBuffer(HexUtil.decodeHex(data.toLowerCase()));
                ctx.write(respByteBuf);
                ctx.flush();
            });

        } else {
            log.info("没有找到上下文！");
            WriterUtil.WriterIO("没有找到上下文！");
        }
    }

    public static void setChannelIsGettingWave(String clientId, String deviceId, String getWaveStatus) {
        if (channelList.containsKey(clientId)) {
            if (getWaveStatus.equals("start")) {
                // 开始取波形
                if (!channelList.get(clientId).getWaveStartDeviceIds().contains(deviceId)) {
                    channelList.get(clientId).getWaveStartDeviceIds().add(deviceId);
                }
                ThreadUtil.execAsync(() -> {
                    // 停留25秒
                    ThreadUtil.sleep(25000);
                    if (channelList.get(clientId).getWaveStartDeviceIds().contains(deviceId)) {
                        channelList.get(clientId).getWaveStartDeviceIds().remove(deviceId);
                    }
                });
            }

            if (getWaveStatus.equals("run")) {
                // 已经开始
                if (!channelList.get(clientId).getWaveRunDeviceIds().contains(deviceId)) {
                    channelList.get(clientId).getWaveRunDeviceIds().add(deviceId);
                }
                if (channelList.get(clientId).getWaveStartDeviceIds().contains(deviceId)) {
                    channelList.get(clientId).getWaveStartDeviceIds().remove(deviceId);
                }
            }

            if (getWaveStatus.equals("end")) {
                if (channelList.get(clientId).getWaveStartDeviceIds().contains(deviceId)) {
                    channelList.get(clientId).getWaveStartDeviceIds().remove(deviceId);
                }
                if (channelList.get(clientId).getWaveRunDeviceIds().contains(deviceId)) {
                    channelList.get(clientId).getWaveRunDeviceIds().remove(deviceId);
                }
            }
        }
    }

    public static int addDevice(String clientId, String deviceId) {
        if (!channelList.get(clientId).getDeviceIds().contains(deviceId)) {
            channelList.get(clientId).getDeviceIds().add(deviceId);
        }
        return channelList.get(clientId).getDeviceIds().indexOf(deviceId);
    }

    public static Boolean getChannelIsGettingWave(String clientId) {
        if (channelList.containsKey(clientId)) {
            return (channelList.get(clientId).getWaveStartDeviceIds().size() + channelList.get(clientId).getWaveRunDeviceIds().size()) > 0;
        }
        return null;
    }

    public static Boolean getChannelIsGettingWave(String clientId, String deviceId) {
        if (channelList.containsKey(clientId)) {
            return ((TcpServerHandler.getChannelList().get(clientId).getWaveRunDeviceIds().contains(deviceId)) ||
                    (TcpServerHandler.getChannelList().get(clientId).getWaveStartDeviceIds().contains(deviceId)));
        }
        return false;
    }

    public static ConcurrentHashMap<String, Channel> getChannelList() {
        return channelList;
    }


}
