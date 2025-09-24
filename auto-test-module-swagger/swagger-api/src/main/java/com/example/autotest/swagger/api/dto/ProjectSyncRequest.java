package com.example.autotest.swagger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Swagger 项目同步请求。
 */
@Data
@Schema(description = "Swagger 项目同步请求")
public class ProjectSyncRequest {

    @Schema(description = "触发类型：1-手动，2-Webhook 等", example = "1")
    private Integer triggerType = 1;

    @Schema(description = "可选的覆盖 Swagger 内容（为空则按项目配置拉取）", example = "{\n  \"openapi\": \"3.0.1\"\n}")
    private String overrideSource;

    @Schema(description = "同步备注，便于复盘", example = "紧急修复 definition 字段缺失")
    private String remark;
}
