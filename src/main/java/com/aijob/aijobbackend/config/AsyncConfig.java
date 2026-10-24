package com.aijob.aijobbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置：简历分析任务专用。
 */
@Configuration
public class AsyncConfig {

    /** 核心线程数：单次 Dify 调用耗时较长，线程数不宜过大 */
    private static final int CORE_POOL_SIZE = 2;

    /** 最大线程数 */
    private static final int MAX_POOL_SIZE = 4;

    /** 等待队列容量 */
    private static final int QUEUE_CAPACITY = 50;

    /** 空闲线程存活时间（秒） */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /** 应用关闭时等待任务执行的最长时间（秒） */
    private static final int AWAIT_TERMINATION_SECONDS = 30;

    /**
     * 简历分析线程池。
     * <p>
     * 队列满时由提交线程（Tomcat 线程）直接执行，保证任务不被丢弃；
     * 若并发量持续超过「最大线程数 + 队列容量」，该请求会同步等待 Dify 返回。
     */
    @Bean("analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("resume-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}
