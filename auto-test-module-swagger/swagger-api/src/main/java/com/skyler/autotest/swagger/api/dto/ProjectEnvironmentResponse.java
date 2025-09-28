package com.skyler.autotest.swagger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Swagger 项目环境响应对象。
 */
@Data
@Schema(description = "Swagger 项目环境响应")
public class ProjectEnvironmentResponse {

    @Schema(description = "环境编号", example = "2001")
    private Long id;

    @Schema(description = "所属项目编号", example = "1001")
    private Long projectId;

    @Schema(description = "环境名称", example = "SIT")
    private String name;

    @Schema(description = "环境类型：1-手工维护，2-继承", example = "1")
    private Integer envType;

    @Schema(description = "环境 Host/BaseUrl", example = "https://sit.example.com")
    private String host;

    @Schema(description = "公共 Header 配置")
    private Map<String, String> headers;

    @Schema(description = "环境级变量映射")
    private Map<String, Object> variables;

    @Schema(description = "是否默认环境", example = "true")
    private Boolean isDefault;

    @Schema(description = "启用状态：1-启用，0-停用", example = "1")
    private Integer status;

    @Schema(description = "环境备注", example = "默认走 SIT 集群")
    private String remark;

    @Schema(description = "创建时间", example = "2025-01-11T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "最后更新时间", example = "2025-01-12T09:30:00")
    private LocalDateTime updateTime;
}
