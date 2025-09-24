package com.example.autotest.diff.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_diff_snapshot")
public class DiffSnapshotDO extends BaseDO {
    private Long projectId;
    private String sourceType;
    private Long sourceRefId;
    private Long relatedId;
    private String diffType;
    private String diffPayload;
    private Boolean needReview;
    private Long reviewerId;
    private String reviewerName;
    private Integer reviewStatus;
    private String reviewComment;
}
