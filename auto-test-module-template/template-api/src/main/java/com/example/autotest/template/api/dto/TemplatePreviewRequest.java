package com.example.autotest.template.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 模板预览请求。
 */
@Data
@Schema(description = "模板预览请求")
public class TemplatePreviewRequest {

    @Schema(description = "变量映射，key 为变量名", example = "{\"orderId\":123456789}")
    private Map<String, Object> variables;
}
