package com.skyler.autotest.diff.converter;

import com.skyler.autotest.diff.api.dto.DiffSnapshotResponse;
import com.skyler.autotest.diff.dal.dataobject.DiffSnapshotDO;

/**
 * 差异实体与 DTO 的转换工具。
 */
public class DiffConverter {

    private DiffConverter() {
    }

    /**
     * 将差异快照实体映射为响应对象，便于 Controller 直接返回。
     */
    public static DiffSnapshotResponse toResponse(DiffSnapshotDO diff) {
        if (diff == null) {
            return null;
        }
        DiffSnapshotResponse response = new DiffSnapshotResponse();
        response.setId(diff.getId());
        response.setProjectId(diff.getProjectId());
        response.setSourceType(diff.getSourceType());
        response.setSourceRefId(diff.getSourceRefId());
        response.setRelatedId(diff.getRelatedId());
        response.setDiffType(diff.getDiffType());
        response.setDiffPayload(diff.getDiffPayload());
        response.setNeedReview(diff.getNeedReview());
        response.setReviewStatus(diff.getReviewStatus());
        response.setReviewComment(diff.getReviewComment());
        response.setCreateTime(diff.getCreateTime());
        return response;
    }
}
