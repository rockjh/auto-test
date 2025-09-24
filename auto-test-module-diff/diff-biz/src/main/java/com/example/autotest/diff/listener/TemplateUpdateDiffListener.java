package com.example.autotest.diff.listener;

import com.example.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.example.autotest.diff.dal.mapper.DiffSnapshotMapper;
import com.example.autotest.infra.core.id.BizIdGenerator;
import com.example.autotest.infra.event.TemplateUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监听模板变更事件，生成差异快照并标记复核。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateUpdateDiffListener {

    private final DiffSnapshotMapper diffSnapshotMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleTemplateUpdate(TemplateUpdateEvent event) {
        DiffSnapshotDO snapshot = new DiffSnapshotDO();
        snapshot.setId(bizIdGenerator.nextId());
        snapshot.setTenantId(event.getTenantId());
        snapshot.setProjectId(event.getProjectId());
        snapshot.setSourceType("template");
        snapshot.setSourceRefId(event.getVariantId());
        snapshot.setDiffType(event.getChangeType());
        snapshot.setDiffPayload(buildPayload(event));
        snapshot.setNeedReview(event.isNeedReview());
        snapshot.setReviewStatus(0);
        diffSnapshotMapper.insert(snapshot);
        log.debug("Template diff captured: variantId={}, changeType={}", event.getVariantId(), event.getChangeType());
    }

    private String buildPayload(TemplateUpdateEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("variantId", event.getVariantId());
        payload.put("groupId", event.getGroupId());
        payload.put("variantType", event.getVariantType());
        payload.put("changeType", event.getChangeType());
        payload.put("curlTemplate", event.getCurlTemplate());
        payload.put("paramRules", event.getParamRules());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize template diff payload", ex);
        }
    }
}
