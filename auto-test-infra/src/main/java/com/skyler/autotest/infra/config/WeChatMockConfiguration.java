package com.skyler.autotest.infra.config;

import com.binarywang.spring.starter.wxjava.mp.properties.WxMpProperties;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开发阶段的微信能力兜底配置，提供占位 Bean 避免未配置凭证导致启动失败。
 */
@Configuration
@ConditionalOnClass(WxMpService.class)
public class WeChatMockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WxMpService wxMpService() {
        WxMpConfigStorage configStorage = buildMockConfig();
        WxMpServiceImpl service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(configStorage);
        return service;
    }

    @Bean
    @ConditionalOnMissingBean
    public WxMpProperties wxMpProperties() {
        WxMpProperties properties = new WxMpProperties();
        properties.setAppId("mock-app-id");
        properties.setSecret("mock-app-secret");
        properties.setToken("mock-token");
        properties.setAesKey("mock-aes");
        return properties;
    }

    private WxMpConfigStorage buildMockConfig() {
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId("mock-app-id");
        config.setSecret("mock-app-secret");
        return config;
    }
}
