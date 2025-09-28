package com.skyler.autotest.scenario.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场景步骤之间的变量映射配置，描述如何从上一步响应提取值并写入下一个请求。
 */
@Data
@Schema(description = "场景步骤变量映射规则")
public class ScenarioVariableMappingRule {

    @Schema(description = "JSONPath 表达式，指向上一步响应中的字段", example = "$.data.orderId")
    private String sourcePath;

    @Schema(description = "请求变量标识，支持 path./query./header./body. 前缀", example = "query.orderId")
    private String targetKey;

    @Schema(description = "备注信息，便于说明该变量用途", example = "订单号用于支付步骤")
    private String remark;
}
