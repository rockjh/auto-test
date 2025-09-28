package com.skyler.autotest.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 执行器线程池配置，统一设置线程池参数与拒绝策略。
 */
@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfig {

    /**
     * 执行器线程池，统一命名与拒绝策略，便于监控排查。
     */
    @Bean(name = "executorThreadPool")
    @ConditionalOnMissingBean(ThreadPoolTaskExecutor.class)
    public ThreadPoolTaskExecutor executorThreadPool(ThreadPoolProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCoreSize());
        executor.setMaxPoolSize(properties.getMaxSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
