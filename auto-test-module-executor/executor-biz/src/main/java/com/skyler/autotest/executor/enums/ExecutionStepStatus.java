package com.skyler.autotest.executor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 执行详情（步骤）状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum ExecutionStepStatus {

    /** 步骤执行中。 */
    RUNNING(0),

    /** 步骤执行成功。 */
    SUCCESS(1),

    /** 步骤执行失败。 */
    FAILED(2),

    /** 步骤被跳过。 */
    SKIPPED(3);

    private final int code;
}
