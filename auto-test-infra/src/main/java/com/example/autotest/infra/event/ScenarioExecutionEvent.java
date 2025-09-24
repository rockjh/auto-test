package com.example.autotest.infra.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 场景执行结束后发布的事件，供通知、统计等扩展能力消费。
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "场景执行事件载荷")
public class ScenarioExecutionEvent {

    @Schema(description = "租户编号")
    private final Long tenantId;

    @Schema(description = "执行记录 ID")
    private final Long executionId;

    @Schema(description = "场景 ID")
    private final Long scenarioId;

    @Schema(description = "执行状态：0排队/1执行中/2成功/3失败/4取消")
    private final Integer status;

    @Schema(description = "执行耗时，单位毫秒")
    private final Long durationMs;
}
