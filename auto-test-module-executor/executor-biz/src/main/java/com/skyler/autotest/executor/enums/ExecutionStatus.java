package com.skyler.autotest.executor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 执行主表状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum ExecutionStatus {

    /** 排队中。 */
    QUEUED(0),

    /** 执行中。 */
    RUNNING(1),

    /** 执行成功。 */
    SUCCESS(2),

    /** 执行失败。 */
    FAILED(3),

    /** 已取消。 */
    CANCELLED(4);

    private final int code;
}
