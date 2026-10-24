package com.aijob.aijobbackend.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 配置：用于调用 Dify 等外部 HTTP 服务。
 * 大模型接口耗时较长，读超时放宽到 120 秒（可通过 dify.timeout-seconds 调整）。
 */
@Configuration
public class WebClientConfig {

    /** 连接超时（毫秒），建连失败通常很快，无需太长 */
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;

    /** 响应体最大缓冲，简历分析结果可能较长 */
    private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024;

    @Value("${dify.timeout-seconds:120}")
    private long timeoutSeconds;

    @Bean
    public WebClient difyWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}
