package com.skyler.autotest.swagger.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Swagger 项目创建请求。
 */
@Data
@Schema(description = "Swagger 项目创建请求")
public class ProjectCreateRequest {

    @NotBlank
    @Schema(description = "项目名称", example = "支付中心")
    private String name;

    @NotBlank
    @Schema(description = "Swagger 文档的来源地址或内容", example = "https://example.com/openapi.yaml")
    private String swaggerSource;

    @NotNull
    @Schema(description = "Swagger 来源类型：1-URL，2-JSON 文本，3-文件上传", example = "1")
    private Integer swaggerType;
}
