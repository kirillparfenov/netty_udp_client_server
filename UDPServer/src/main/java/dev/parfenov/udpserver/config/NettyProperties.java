package dev.parfenov.udpserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "netty.server")
public record NettyProperties(int port,
                              int bossThreads,
                              int workerThreads,
                              String bossThreadName,
                              String workerThreadName) {}
