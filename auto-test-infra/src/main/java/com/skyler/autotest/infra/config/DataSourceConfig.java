package com.skyler.autotest.infra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 数据源配置，封装 Hikari 连接池，并暴露自定义池参数。
 */
@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, DataSourcePoolProperties.class})
public class DataSourceConfig {

    /**
     * 构建 Hikari 数据源，并应用定制的连接池参数。
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(DataSourceProperties properties, DataSourcePoolProperties poolProperties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        config.setMaximumPoolSize(poolProperties.getMaxPoolSize());
        config.setMinimumIdle(poolProperties.getMinIdle());
        config.setPoolName(poolProperties.getPoolName());
        return new HikariDataSource(config);
    }
}
