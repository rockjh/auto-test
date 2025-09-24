package com.example.autotest.swagger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Swagger 同步差异信息响应。
 */
@Data
@Schema(description = "Swagger 同步差异信息响应")
public class SwaggerDiffResponse {

    @Schema(description = "同步任务编号", example = "202501150001")
    private Long syncId;

    @Schema(description = "同步状态：1-进行中，2-成功，3-失败", example = "2")
    private Integer status;

    @Schema(description = "差异摘要，包含新增/变更/删除统计", example = "新增接口 3 个，字段差异 2 项")
    private String diffSummary;

    @Schema(description = "同步开始时间", example = "2025-01-15T10:15:30")
    private LocalDateTime startTime;

    @Schema(description = "同步完成时间", example = "2025-01-15T10:15:45")
    private LocalDateTime endTime;

    @Schema(description = "错误信息，仅同步失败时返回", example = "解析 path '/api/order' 缺少 method 定义")
    private String errorMessage;
}
