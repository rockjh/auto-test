package com.example.autotest.diff.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 差异复核请求。
 */
@Data
@Schema(description = "差异复核请求")
public class DiffReviewRequest {

    @Schema(description = "复核状态：1-通过，2-驳回", example = "1")
    private Integer reviewStatus;

    @Schema(description = "复核备注", example = "确认模板调整无误")
    private String reviewComment;
}
