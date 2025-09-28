package com.skyler.autotest.diff.api;

import com.skyler.autotest.diff.api.dto.DiffReviewRequest;
import com.skyler.autotest.diff.api.dto.DiffSnapshotResponse;

import java.util.List;

/**
 * 差异复核领域服务。
 */
public interface DiffService {

    /**
     * 查询待复核差异。
     *
     * @param projectId 项目编号，可为空
     * @return 待复核差异列表
     */
    List<DiffSnapshotResponse> listPending(Long projectId);

    /**
     * 提交差异复核结果。
     *
     * @param diffId  差异快照主键
     * @param request 复核请求
     * @return 最新差异信息
     */
    DiffSnapshotResponse review(Long diffId, DiffReviewRequest request);
}
