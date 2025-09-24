package com.example.autotest.diff.converter;

import com.example.autotest.diff.api.dto.DiffSnapshotResponse;
import com.example.autotest.diff.dal.dataobject.DiffSnapshotDO;

public class DiffConverter {

    private DiffConverter() {
    }

    public static DiffSnapshotResponse toResponse(DiffSnapshotDO diff) {
        if (diff == null) {
            return null;
        }
        DiffSnapshotResponse response = new DiffSnapshotResponse();
        response.setId(diff.getId());
        response.setProjectId(diff.getProjectId());
        response.setSourceType(diff.getSourceType());
        response.setDiffType(diff.getDiffType());
        response.setDiffPayload(diff.getDiffPayload());
        response.setNeedReview(diff.getNeedReview());
        response.setReviewStatus(diff.getReviewStatus());
        response.setReviewComment(diff.getReviewComment());
        response.setCreateTime(diff.getCreateTime());
        return response;
    }
}
