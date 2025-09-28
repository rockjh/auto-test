package com.skyler.autotest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 自动生成 Swagger 接口测试场景系统的启动入口，负责启动所有业务模块并加载 RuoYi 公共能力。
 */
@SpringBootApplication(scanBasePackages = {
        "com.skyler.autotest",
        "cn.iocoder.yudao"
})
public class AutoTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTestApplication.class, args);
    }
}
