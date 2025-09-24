package com.example.autotest.template.controller;

import com.example.autotest.infra.core.web.CommonResult;
import com.example.autotest.template.api.TemplateService;
import com.example.autotest.template.api.dto.CurlVariantResponse;
import com.example.autotest.template.api.dto.TemplatePreviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模板管理接口，负责 `curl` 模板的查询、重生成与预览。
 */
@RestController
@RequestMapping("/api/template/variants")
@RequiredArgsConstructor
@Tag(name = "模板管理", description = "`curl` 模板查询与维护接口")
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 按接口分组查询模板列表。
     *
     * @param groupId 接口分组编号
     * @return 模板列表
     */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "查询分组模板", description = "根据接口分组编号返回所有模板版本")
    public CommonResult<List<CurlVariantResponse>> listByGroup(@Parameter(description = "接口分组编号", example = "2001001")
                                                               @PathVariable Long groupId) {
        return CommonResult.success(templateService.listVariantsByGroup(groupId));
    }

    /**
     * 重新生成指定分组的最小模板。
     *
     * @param groupId 接口分组编号
     * @return 最新生成的最小模板
     */
    @PostMapping("/group/{groupId}/minimal")
    @Operation(summary = "重新生成最小模板", description = "基于最新 Swagger 元数据生成默认最小模板并覆盖旧版本")
    public CommonResult<CurlVariantResponse> regenerateMinimal(@Parameter(description = "接口分组编号", example = "2001001")
                                                               @PathVariable Long groupId) {
        return CommonResult.success(templateService.regenerateMinimalVariant(groupId));
    }

    /**
     * 预览指定模板的 `curl` 内容。
     *
     * @param variantId 模板编号
     * @param request   变量覆盖配置
     * @return 渲染后的 `curl` 文本
     */
    @PostMapping("/{variantId}/preview")
    @Operation(summary = "预览模板", description = "根据变量映射渲染模板，返回可执行 `curl` 命令")
    public CommonResult<String> preview(@Parameter(description = "模板编号", example = "301001")
                                        @PathVariable Long variantId,
                                        @RequestBody(required = false) TemplatePreviewRequest request) {
        return CommonResult.success(templateService.preview(variantId, request));
    }
}
