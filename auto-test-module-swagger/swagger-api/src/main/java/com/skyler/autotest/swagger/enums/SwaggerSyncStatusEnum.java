package com.skyler.autotest.swagger.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Swagger 同步任务状态枚举，对应 {@code autotest_swagger_sync.status} 字段。
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerSyncStatusEnum {

    /** 同步进行中。 */
    RUNNING(0, "进行中"),

    /** 同步完成且成功。 */
    SUCCESS(1, "成功"),

    /** 同步失败。 */
    FAILED(2, "失败");

    private final int code;
    private final String description;
}

