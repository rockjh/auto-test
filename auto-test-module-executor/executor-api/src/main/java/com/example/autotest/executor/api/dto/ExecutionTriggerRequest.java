package com.example.autotest.executor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 执行触发请求。
 */
@Data
@Schema(description = "执行触发请求")
public class ExecutionTriggerRequest {

    @Schema(description = "场景编号", example = "5001001")
    private Long scenarioId;

    @Schema(description = "执行环境编号", example = "3001")
    private Long envId;

    @Schema(description = "触发备注", example = "回归覆盖支付链路")
    private String remark;
}
