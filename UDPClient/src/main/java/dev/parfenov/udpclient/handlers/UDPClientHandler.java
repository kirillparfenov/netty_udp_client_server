package dev.parfenov.udpclient.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class UDPClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(UDPClientHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        var msg = Unpooled.copiedBuffer("Проверка связи", StandardCharsets.UTF_8);
        ctx.writeAndFlush(msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        log.info("Получено сообщение: {}", msg);
    }
}
