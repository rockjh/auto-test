package com.skyler.autotest.infra.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 模板生成或调整后发布的事件，通知场景与差异模块联动处理。
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "模板变更事件载荷")
public class TemplateUpdateEvent {

    @Schema(description = "租户编号")
    private final Long tenantId;

    @Schema(description = "项目编号")
    private final Long projectId;

    @Schema(description = "所属接口分组 ID")
    private final Long groupId;

    @Schema(description = "模板 ID")
    private final Long variantId;

    @Schema(description = "模板类型 minimal/full/custom")
    private final String variantType;

    @Schema(description = "变更类型：CREATE/UPDATE/DELETE 等")
    private final String changeType;

    @Schema(description = "最新 curl 模板内容")
    private final String curlTemplate;

    @Schema(description = "最新参数规则 JSON")
    private final String paramRules;

    @Schema(description = "是否需要复核")
    private final boolean needReview;
}
