package com.skyler.autotest.swagger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * Swagger 项目环境更新请求。
 */
@Data
@Schema(description = "Swagger 项目环境更新请求")
public class ProjectEnvironmentUpdateRequest {

    @NotNull
    @Schema(description = "所属项目编号", example = "1001")
    private Long projectId;

    @NotBlank
    @Schema(description = "环境名称", example = "SIT")
    private String name;

    @Schema(description = "环境类型：1-手工维护，2-继承", example = "1")
    private Integer envType;

    @NotBlank
    @Schema(description = "环境 Host/BaseUrl", example = "https://sit.example.com")
    private String host;

    @Schema(description = "公共 Header 配置", example = "{\"Authorization\":\"Bearer ${token}\"}")
    private Map<String, String> headers;

    @Schema(description = "环境级变量映射", example = "{\"tenant\":\"qa\"}")
    private Map<String, Object> variables;

    @Schema(description = "是否设为默认环境", example = "true")
    private Boolean isDefault;

    @Schema(description = "启用状态：1-启用，0-停用", example = "1")
    private Integer status;

    @Schema(description = "环境备注", example = "默认走 SIT 集群")
    private String remark;
}
