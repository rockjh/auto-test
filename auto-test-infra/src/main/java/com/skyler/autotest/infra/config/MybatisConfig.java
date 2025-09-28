package com.skyler.autotest.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础配置，统一扫描业务与 RuoYi 公共模块的 Mapper。
 */
@Configuration
@MapperScan(basePackages = {
        "com.skyler.autotest.**.mapper",
        "cn.iocoder.yudao.module.**.dal.mysql"
})
public class MybatisConfig {
    // 此处仅保留 Mapper 扫描配置，MyBatis-Plus 拦截器由 RuoYi 的 YudaoMybatisAutoConfiguration 统一提供。
}
