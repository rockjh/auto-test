package com.example.autotest.scenario.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 场景创建/更新请求。
 */
@Data
@Schema(description = "场景创建或更新请求")
public class ScenarioCreateRequest {

    @NotNull
    @Schema(description = "所属项目编号", example = "1001")
    private Long projectId;

    @NotBlank
    @Schema(description = "场景名称", example = "订单支付成功链路")
    private String name;

    @Schema(description = "默认执行环境编号", example = "3001")
    private Long defaultEnvId;

    @Schema(description = "场景备注", example = "覆盖支付 + 退款 Happy Path")
    private String remark;

    @NotEmpty
    @Schema(description = "场景步骤列表")
    private List<ScenarioStepRequest> steps;

    /**
     * 场景步骤定义。
     */
    @Data
    @Schema(description = "场景步骤定义")
    public static class ScenarioStepRequest {

        @NotNull
        @Schema(description = "引用的模板编号", example = "301001")
        private Long curlVariantId;

        @Schema(description = "步骤别名，便于识别", example = "提交订单")
        private String stepAlias;

        @Schema(description = "步骤顺序，从 1 开始", example = "1")
        private Integer orderNo;

        @Schema(description = "变量映射配置 JSON", example = "{\"$.data.orderId\":\"orderId\"}")
        private String variableMapping;

        @Schema(description = "调用选项配置，如重试策略", example = "{\"retry\":2}")
        private String invokeOptions;
    }
}
