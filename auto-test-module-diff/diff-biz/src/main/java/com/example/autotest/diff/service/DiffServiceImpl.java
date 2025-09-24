package com.example.autotest.diff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.autotest.diff.api.DiffService;
import com.example.autotest.diff.api.dto.DiffReviewRequest;
import com.example.autotest.diff.api.dto.DiffSnapshotResponse;
import com.example.autotest.diff.converter.DiffConverter;
import com.example.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.example.autotest.diff.dal.mapper.DiffSnapshotMapper;
import com.example.autotest.infra.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiffServiceImpl implements DiffService {

    private final DiffSnapshotMapper diffSnapshotMapper;

    @Override
    public List<DiffSnapshotResponse> listPending(Long projectId) {
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
        DiffSnapshotDO diff = diffSnapshotMapper.selectById(diffId);
        if (diff == null) {
            throw new BizException(404, "diff not found");
        }
        diff.setNeedReview(false);
        diff.setReviewStatus(request.getReviewStatus());
        diff.setReviewComment(request.getReviewComment());
        diffSnapshotMapper.updateById(diff);
        return DiffConverter.toResponse(diff);
    }
}
