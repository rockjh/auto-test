package com.skyler.autotest.scenario.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 场景状态枚举，统一管理状态值与语义。
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioStatus {

    /** 草稿状态，允许继续编辑。 */
    DRAFT(0),

    /** 已发布状态，可用于执行。 */
    PUBLISHED(1),

    /** 已废弃状态，保留历史数据。 */
    ARCHIVED(2);

    private final int code;
}
