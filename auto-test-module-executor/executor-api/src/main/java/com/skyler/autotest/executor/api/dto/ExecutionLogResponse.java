package com.skyler.autotest.executor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行日志响应条目。
 */
@Data
@Schema(description = "执行日志响应条目")
public class ExecutionLogResponse {

    @Schema(description = "日志编号", example = "9100001")
    private Long id;

    @Schema(description = "关联执行记录编号", example = "9001001")
    private Long executionId;

    @Schema(description = "关联步骤编号", example = "6002001")
    private Long scenarioStepId;

    @Schema(description = "日志时间", example = "2025-01-15T12:30:05")
    private LocalDateTime logTime;

    @Schema(description = "日志级别", example = "INFO")
    private String level;

    @Schema(description = "日志内容", example = "步骤 1 执行成功")
    private String message;

    @Schema(description = "附加信息 JSON 字符串", example = "{\"command\":\"curl -X GET ...\"}")
    private String extra;

    @Schema(description = "通知渠道", example = "EMAIL")
    private String notificationChannel;

    @Schema(description = "通知状态", example = "0")
    private Integer notificationStatus;
}
