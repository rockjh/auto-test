package com.skyler.autotest.swagger.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Swagger 项目同步状态，对应 {@code autotest_project.sync_status} 字段。
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerProjectSyncStatusEnum {

    /** 尚未执行同步。 */
    PENDING(0, "待同步"),

    /** 最近一次同步成功。 */
    SUCCESS(1, "同步成功"),

    /** 最近一次同步失败。 */
    FAILED(2, "同步失败");

    private final int code;
    private final String description;
}

