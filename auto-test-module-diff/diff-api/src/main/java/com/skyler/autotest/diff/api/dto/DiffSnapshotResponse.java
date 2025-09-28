package com.skyler.autotest.diff.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 差异快照响应。
 */
@Data
@Schema(description = "差异快照")
public class DiffSnapshotResponse {

    @Schema(description = "差异快照编号", example = "8001001")
    private Long id;

    @Schema(description = "项目编号", example = "1001")
    private Long projectId;

    @Schema(description = "差异来源类型", example = "swagger")
    private String sourceType;

    @Schema(description = "差异来源关联 ID", example = "2001")
    private Long sourceRefId;

    @Schema(description = "差异相关实体 ID", example = "3001")
    private Long relatedId;

    @Schema(description = "差异类型", example = "INTERFACE_CHANGE")
    private String diffType;

    @Schema(description = "差异内容快照 JSON", example = "{\"added\":3,\"updated\":2}")
    private String diffPayload;

    @Schema(description = "是否需要复核", example = "true")
    private Boolean needReview;

    @Schema(description = "复核状态：0-待处理，1-通过，2-驳回", example = "0")
    private Integer reviewStatus;

    @Schema(description = "复核备注", example = "等待模板调整")
    private String reviewComment;

    @Schema(description = "创建时间", example = "2025-01-15T09:30:00")
    private LocalDateTime createTime;
}
