package com.example.autotest.executor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行步骤详情。
 */
@Data
@Schema(description = "执行步骤详情")
public class ExecutionDetailResponse {

    @Schema(description = "执行步骤记录编号", example = "9100001")
    private Long id;

    @Schema(description = "场景步骤编号", example = "7001001")
    private Long scenarioStepId;

    @Schema(description = "步骤顺序", example = "1")
    private Integer stepOrder;

    @Schema(description = "步骤状态：1-成功，2-失败，3-跳过", example = "1")
    private Integer status;

    @Schema(description = "请求快照", example = "{\"path\":\"/api/order\"}")
    private String requestSnapshot;

    @Schema(description = "响应快照", example = "{\"code\":0,\"data\":{...}}")
    private String responseSnapshot;

    @Schema(description = "错误信息，仅失败时存在", example = "超时 5s")
    private String errorMessage;

    @Schema(description = "步骤开始时间", example = "2025-01-15T12:30:01")
    private LocalDateTime startTime;

    @Schema(description = "步骤结束时间", example = "2025-01-15T12:30:03")
    private LocalDateTime endTime;
}
