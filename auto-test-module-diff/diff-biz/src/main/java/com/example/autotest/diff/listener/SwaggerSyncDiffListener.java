package com.example.autotest.diff.listener;

import com.example.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.example.autotest.diff.dal.mapper.DiffSnapshotMapper;
import com.example.autotest.infra.core.id.BizIdGenerator;
import com.example.autotest.infra.event.SwaggerSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 监听 Swagger 同步事件，落地整体 diff 快照，供复核看板使用。
 */
@Component
@RequiredArgsConstructor
public class SwaggerSyncDiffListener {

    private final DiffSnapshotMapper diffSnapshotMapper;
    private final BizIdGenerator bizIdGenerator;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleSwaggerSync(SwaggerSyncEvent event) {
        DiffSnapshotDO snapshot = new DiffSnapshotDO();
        snapshot.setId(bizIdGenerator.nextId());
        snapshot.setTenantId(event.getTenantId());
        snapshot.setProjectId(event.getProjectId());
        snapshot.setSourceType("swagger");
        snapshot.setSourceRefId(event.getSyncId());
        snapshot.setDiffType("sync");
        snapshot.setDiffPayload(event.getDiffSummary());
        boolean needReview = (event.getUpdatedGroupIds() != null && !event.getUpdatedGroupIds().isEmpty())
                || (event.getRemovedGroupIds() != null && !event.getRemovedGroupIds().isEmpty());
        snapshot.setNeedReview(needReview);
        snapshot.setReviewStatus(0);
        diffSnapshotMapper.insert(snapshot);
    }
}
