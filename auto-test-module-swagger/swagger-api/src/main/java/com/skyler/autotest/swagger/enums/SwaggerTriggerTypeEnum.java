package com.skyler.autotest.swagger.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Swagger 同步触发类型枚举，对应 {@code autotest_swagger_sync.trigger_type} 字段。
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerTriggerTypeEnum {

    /** 手动触发。 */
    MANUAL(1, "手动"),

    /** 定时任务触发。 */
    SCHEDULED(2, "定时"),

    /** Webhook 或外部事件触发。 */
    WEBHOOK(3, "外部事件");

    private final int code;
    private final String description;

    /**
     * 根据编码解析触发类型，若入参为空或未匹配则回退到手动触发。
     *
     * @param code 枚举编码
     * @return 匹配到的枚举
     */
    public static SwaggerTriggerTypeEnum fromCode(Integer code) {
        if (code == null) {
            return MANUAL;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(MANUAL);
    }
}

