package com.skyler.autotest.template.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * `curl` 模板基本信息响应。
 */
@Data
@Schema(description = "curl 模板信息")
public class CurlVariantResponse {

    @Schema(description = "模板编号", example = "301001")
    private Long id;

    @Schema(description = "所属项目编号", example = "1001")
    private Long projectId;

    @Schema(description = "所属接口分组编号", example = "2001001")
    private Long groupId;

    @Schema(description = "模板类型：minimal/full/custom", example = "minimal")
    private String variantType;

    @Schema(description = "模板正文，包含可替换变量", example = "curl -X GET https://api.example.com/orders/{orderId}")
    private String curlTemplate;

    @Schema(description = "模板参数规则 JSON", example = "{\"orderId\":{\"random\":\"number\"}}")
    private String paramRules;

    @Schema(description = "是否需要人工复核", example = "false")
    private Boolean needReview;
}
