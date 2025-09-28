package com.skyler.autotest.swagger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Swagger 项目基础信息响应。
 */
@Data
@Schema(description = "Swagger 项目基础信息响应")
public class ProjectResponse {

    @Schema(description = "项目编号", example = "1001")
    private Long id;

    @Schema(description = "项目名称", example = "支付中心")
    private String name;

    @Schema(description = "原始 Swagger 来源", example = "https://example.com/openapi.yaml")
    private String swaggerSource;

    @Schema(description = "Swagger 来源类型：1-URL，2-JSON 文本，3-文件上传", example = "1")
    private Integer swaggerType;

    @Schema(description = "当前解析到的 Swagger 版本", example = "1.0.12")
    private String swaggerVersion;

    @Schema(description = "最近同步的内容 Hash", example = "6f5a0b86f271")
    private String swaggerHash;

    @Schema(description = "最近同步状态：0-未开始，1-成功，2-失败", example = "1")
    private Integer syncStatus;

    @Schema(description = "最近同步时间", example = "2025-01-15T10:15:30")
    private LocalDateTime syncTime;
}
