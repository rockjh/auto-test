package com.skyler.autotest.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 提供基础 RestTemplate 实例，用于受控的外部 HTTP 访问。
 */
@Configuration
@EnableConfigurationProperties(RestTemplateProperties.class)
public class RestTemplateConfig {

    /**
     * 构造具备超时控制的 RestTemplate，避免网络调用阻塞业务线程。
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(RestTemplateBuilder builder, RestTemplateProperties properties) {
        // 将远程抓取的连接与读取超时时间置于配置文件，便于运维按需调整。
        return builder
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }
}
