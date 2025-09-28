package com.skyler.autotest.diff.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

/**
 * 差异快照数据对象，对应表 autotest_diff_snapshot。
 */
@Data
@TableName("autotest_diff_snapshot")
public class DiffSnapshotDO extends BaseDO {

    /** 项目编号。 */
    private Long projectId;

    /** 差异来源类型。 */
    private String sourceType;

    /** 差异来源关联 ID。 */
    private Long sourceRefId;

    /** 关联实体 ID。 */
    private Long relatedId;

    /** 差异类型。 */
    private String diffType;

    /** 差异内容快照 JSON。 */
    private String diffPayload;

    /** 是否需要复核。 */
    private Boolean needReview;

    /** 复核人 ID。 */
    private Long reviewerId;

    /** 复核人姓名。 */
    private String reviewerName;

    /** 复核状态。 */
    private Integer reviewStatus;

    /** 复核备注。 */
    private String reviewComment;
}
