package com.skyler.autotest.swagger.api.dto;

import com.skyler.autotest.swagger.enums.SwaggerTriggerTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * Swagger 项目同步请求。
 */
@Data
@Schema(description = "Swagger 项目同步请求")
public class ProjectSyncRequest {

    @Schema(description = "触发类型：1-手动，2-Webhook 等", example = "1")
    private Integer triggerType = SwaggerTriggerTypeEnum.MANUAL.getCode();

    @Schema(description = "可选的覆盖 Swagger 内容（为空则按项目配置拉取）", example = "{\n  \"openapi\": \"3.0.1\"\n}")
    private String overrideSource;

    @Schema(description = "同步备注，便于复盘", example = "紧急修复 definition 字段缺失")
    private String remark;

    @Schema(description = "是否允许根据项目配置远程抓取 Swagger 文档", example = "false")
    private Boolean allowRemoteFetch = Boolean.FALSE;

    @Schema(description = "远程抓取时自定义 Header", example = "{\"X-Tenant\":\"qa\"}")
    private Map<String, String> requestHeaders;
}
