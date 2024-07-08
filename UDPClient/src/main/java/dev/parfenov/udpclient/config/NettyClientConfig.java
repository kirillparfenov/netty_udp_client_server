package dev.parfenov.udpclient.config;

import dev.parfenov.udpclient.handlers.UDPClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NettyProperties.class)
public class NettyClientConfig {
    private static final Logger log = LoggerFactory.getLogger(NettyClientConfig.class);

    /**
     * Конфигурация и запуск netty-сервера
     */
    @Bean
    @SneakyThrows
    public ChannelFuture nettyServer(
            NettyProperties nettyProperties,
            MultithreadEventLoopGroup workerGroup,
            ChannelHandler channelHandler
    ) {
        return new Bootstrap()
                .channel(Epoll.isAvailable() ? EpollDatagramChannel.class : NioDatagramChannel.class)
                .group(workerGroup)
                .handler(channelHandler)
                .connect("localhost", nettyProperties.port())
                .sync()
                .channel()
                .closeFuture()
                .addListener(closed -> {
                    if (closed.isSuccess()) {
                        log.info("Остановка потоков выполнения");
                        workerGroup.shutdownGracefully();
                    }
                });
    }

    /**
     * Worker - группа для обработки задач входящий подключений
     */
    @Bean
    public MultithreadEventLoopGroup workerGroup(
            NettyProperties nettyProperties
    ) {
        return Epoll.isAvailable() ?
                new EpollEventLoopGroup(nettyProperties.workerThreads(), threadFactory(nettyProperties.workerThreadName())) :
                new NioEventLoopGroup(nettyProperties.workerThreads(), threadFactory(nettyProperties.workerThreadName()));
    }

    /**
     * Фабрика создания потоков netty
     */
    private ThreadFactory threadFactory(String threadName) {
        var counter = new AtomicInteger();
        return runnable -> new Thread(runnable, threadName + counter.getAndIncrement());
    }

    /**
     * Настройка пайплайна для входящего подключения
     */
    @Bean
    public ChannelHandler channelHandler() {
        Consumer<DatagramChannel> c = channel -> channel.pipeline().addLast(
                new UDPClientHandler()
        );

        return Epoll.isAvailable()
                ? new ChannelInitializer<EpollDatagramChannel>() {
            @Override
            protected void initChannel(EpollDatagramChannel ch) throws Exception {
                c.accept(ch);
            }
        }
                : new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) throws Exception {
                c.accept(ch);
            }
        };
    }
}
