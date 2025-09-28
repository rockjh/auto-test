package com.skyler.autotest.diff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skyler.autotest.diff.api.DiffService;
import com.skyler.autotest.diff.api.dto.DiffReviewRequest;
import com.skyler.autotest.diff.api.dto.DiffSnapshotResponse;
import com.skyler.autotest.diff.converter.DiffConverter;
import com.skyler.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.skyler.autotest.diff.dal.mapper.DiffSnapshotMapper;
import com.skyler.autotest.diff.enums.DiffReviewStatus;
import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 差异复核服务实现。
 */
@Service
@RequiredArgsConstructor
public class DiffServiceImpl implements DiffService {

    private final DiffSnapshotMapper diffSnapshotMapper;

    @Override
    public List<DiffSnapshotResponse> listPending(Long projectId) {
        // 仅返回待复核记录，按创建时间倒序方便前端展示最新待办
        List<DiffSnapshotDO> list = diffSnapshotMapper.selectList(new LambdaQueryWrapper<DiffSnapshotDO>()
                .eq(projectId != null, DiffSnapshotDO::getProjectId, projectId)
                .eq(DiffSnapshotDO::getNeedReview, true)
                .orderByDesc(DiffSnapshotDO::getCreateTime));
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        return list.stream().map(DiffConverter::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiffSnapshotResponse review(Long diffId, DiffReviewRequest request) {
        // 校验差异是否存在
        DiffSnapshotDO diff = diffSnapshotMapper.selectById(diffId);
        if (diff == null) {
            throw AutoTestErrorCode.DIFF_NOT_FOUND.exception();
        }
        if (request == null) {
            throw AutoTestErrorCode.DIFF_REVIEW_REQUEST_REQUIRED.exception();
        }
        Integer reviewStatus = request.getReviewStatus();
        // 复核状态仅允许通过/驳回，防止出现回退为待处理的情况
        DiffReviewStatus status = DiffReviewStatus.fromCode(reviewStatus)
                .filter(item -> item != DiffReviewStatus.PENDING)
                .orElseThrow(AutoTestErrorCode.DIFF_REVIEW_STATUS_INVALID::exception);
        diff.setNeedReview(Boolean.FALSE);
        diff.setReviewStatus(status.getCode());
        diff.setReviewComment(request.getReviewComment());
        diffSnapshotMapper.updateById(diff);
        return DiffConverter.toResponse(diff);
    }
}
