package com.hbicc.cloud.agent.initializer;

import com.hbicc.cloud.agent.decoder.Decoder;
// import com.hbicc.cloud.agent.decoder.V1Decoder;
// import com.hbicc.cloud.agent.decoder.V2Decoder;
import com.hbicc.cloud.agent.handler.TcpServerHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 添加编解码
        // socketChannel.pipeline().addLast("decoder", new V1Decoder());
        // socketChannel.pipeline().addLast("decoder", new V1Decoder());
        socketChannel.pipeline().addLast("decoder", new Decoder());
        // socketChannel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.US_ASCII));
        socketChannel.pipeline().addLast(new TcpServerHandler());
    }
}