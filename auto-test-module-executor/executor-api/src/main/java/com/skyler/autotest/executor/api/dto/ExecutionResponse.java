package com.skyler.autotest.executor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行详情响应。
 */
@Data
@Schema(description = "执行详情响应")
public class ExecutionResponse {

    @Schema(description = "执行记录编号", example = "9001001")
    private Long id;

    @Schema(description = "关联场景编号", example = "5001001")
    private Long scenarioId;

    @Schema(description = "执行状态：0-排队，1-执行中，2-成功，3-失败，4-取消", example = "2")
    private Integer status;

    @Schema(description = "执行开始时间", example = "2025-01-15T12:30:00")
    private LocalDateTime startTime;

    @Schema(description = "执行结束时间", example = "2025-01-15T12:30:15")
    private LocalDateTime endTime;

    @Schema(description = "执行耗时（毫秒）", example = "15000")
    private Long durationMs;

    @Schema(description = "触发备注", example = "上线前回归")
    private String remark;

    @Schema(description = "步骤执行详情列表")
    private List<ExecutionDetailResponse> steps;
}
