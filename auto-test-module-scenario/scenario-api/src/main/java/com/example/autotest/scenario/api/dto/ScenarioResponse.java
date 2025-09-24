package com.example.autotest.scenario.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景详情响应。
 */
@Data
@Schema(description = "场景详情信息")
public class ScenarioResponse {

    @Schema(description = "场景编号", example = "5001001")
    private Long id;

    @Schema(description = "所属项目编号", example = "1001")
    private Long projectId;

    @Schema(description = "场景名称", example = "订单支付成功链路")
    private String name;

    @Schema(description = "场景状态：0-草稿，1-已发布，2-废弃", example = "1")
    private Integer status;

    @Schema(description = "默认环境编号", example = "3001")
    private Long defaultEnvId;

    @Schema(description = "备注信息", example = "覆盖 1.2.0 版本接口")
    private String remark;

    @Schema(description = "是否需要人工复核", example = "false")
    private Boolean needReview;

    @Schema(description = "创建时间", example = "2025-01-15T11:02:00")
    private LocalDateTime createTime;

    @Schema(description = "场景步骤列表")
    private List<ScenarioStepVO> steps;

    /**
     * 场景步骤静态视图。
     */
    @Data
    @Schema(description = "场景步骤视图")
    public static class ScenarioStepVO {

        @Schema(description = "步骤编号", example = "7001001")
        private Long id;

        @Schema(description = "执行顺序", example = "1")
        private Integer orderNo;

        @Schema(description = "步骤别名", example = "提交订单")
        private String stepAlias;

        @Schema(description = "引用的模板编号", example = "301001")
        private Long curlVariantId;

        @Schema(description = "变量映射配置", example = "{\"$.data.orderId\":\"orderId\"}")
        private String variableMapping;

        @Schema(description = "调用选项配置", example = "{\"retry\":2}")
        private String invokeOptions;
    }
}
