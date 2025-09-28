package com.skyler.autotest.scenario.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场景发布请求。
 */
@Data
@Schema(description = "场景发布请求")
public class ScenarioPublishRequest {

    @Schema(description = "发布备注", example = "覆盖 1.2.0 版本订单接口")
    private String comment;
}
