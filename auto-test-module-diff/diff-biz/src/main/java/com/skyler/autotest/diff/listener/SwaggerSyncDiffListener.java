package com.skyler.autotest.diff.listener;

import com.skyler.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.skyler.autotest.diff.dal.mapper.DiffSnapshotMapper;
import com.skyler.autotest.diff.enums.DiffReviewStatus;
import com.skyler.autotest.diff.enums.DiffSourceType;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.infra.event.SwaggerSyncEvent;
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

    /** Diff 类型：Swagger 同步任务输出的整体差异。 */
    private static final String SWAGGER_SYNC_DIFF_TYPE = "sync";

    private final DiffSnapshotMapper diffSnapshotMapper;
    private final BizIdGenerator bizIdGenerator;

    /**
     * 将 Swagger 同步结果转化为差异快照，若存在结构变更则自动标记复核。
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleSwaggerSync(SwaggerSyncEvent event) {
        DiffSnapshotDO snapshot = new DiffSnapshotDO();
        snapshot.setId(bizIdGenerator.nextId());
        snapshot.setTenantId(event.getTenantId());
        snapshot.setProjectId(event.getProjectId());
        snapshot.setSourceType(DiffSourceType.SWAGGER.getType());
        snapshot.setSourceRefId(event.getSyncId());
        snapshot.setDiffType(SWAGGER_SYNC_DIFF_TYPE);
        snapshot.setDiffPayload(event.getDiffSummary());
        boolean needReview = (event.getUpdatedGroupIds() != null && !event.getUpdatedGroupIds().isEmpty())
                || (event.getRemovedGroupIds() != null && !event.getRemovedGroupIds().isEmpty());
        snapshot.setNeedReview(needReview);
        snapshot.setReviewStatus(DiffReviewStatus.PENDING.getCode());
        diffSnapshotMapper.insert(snapshot);
    }
}
