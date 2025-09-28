package com.skyler.autotest.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据源连接池自定义配置。
 */
@ConfigurationProperties(prefix = "autotest.datasource.pool")
public class DataSourcePoolProperties {

    /** 最大连接数。 */
    private int maxPoolSize = 16;

    /** 最小空闲连接数。 */
    private int minIdle = 4;

    /** 连接池名称。 */
    private String poolName = "autotest-hikari";

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }
}
