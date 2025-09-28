package com.skyler.autotest.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一维护执行器线程池配置，支持通过配置中心灵活调整。
 */
@ConfigurationProperties(prefix = "autotest.executor")
public class ThreadPoolProperties {

    /** 核心线程数。 */
    private int coreSize = 4;

    /** 最大线程数。 */
    private int maxSize = 8;

    /** 队列长度。 */
    private int queueCapacity = 100;

    /** 线程名前缀。 */
    private String threadNamePrefix = "autotest-exec-";

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
}
