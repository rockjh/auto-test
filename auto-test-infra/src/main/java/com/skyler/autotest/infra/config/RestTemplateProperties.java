package com.skyler.autotest.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RestTemplate 相关参数配置。
 */
@ConfigurationProperties(prefix = "autotest.http")
public class RestTemplateProperties {

    /** 连接超时时间。 */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** 读取超时时间。 */
    private Duration readTimeout = Duration.ofSeconds(15);

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
