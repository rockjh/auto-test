package com.skyler.autotest.diff.listener;

import com.skyler.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.skyler.autotest.diff.dal.mapper.DiffSnapshotMapper;
import com.skyler.autotest.diff.enums.DiffReviewStatus;
import com.skyler.autotest.diff.enums.DiffSourceType;
import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.infra.event.TemplateUpdateEvent;
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

    /** JSON 字段名称常量，集中维护避免散落硬编码。 */
    private static final String KEY_VARIANT_ID = "variantId";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_VARIANT_TYPE = "variantType";
    private static final String KEY_CHANGE_TYPE = "changeType";
    private static final String KEY_CURL_TEMPLATE = "curlTemplate";
    private static final String KEY_PARAM_RULES = "paramRules";

    private final DiffSnapshotMapper diffSnapshotMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;

    /**
     * 捕获模板变更并记录差异，必要时触发人工复核。
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleTemplateUpdate(TemplateUpdateEvent event) {
        DiffSnapshotDO snapshot = new DiffSnapshotDO();
        snapshot.setId(bizIdGenerator.nextId());
        snapshot.setTenantId(event.getTenantId());
        snapshot.setProjectId(event.getProjectId());
        snapshot.setSourceType(DiffSourceType.TEMPLATE.getType());
        snapshot.setSourceRefId(event.getVariantId());
        snapshot.setDiffType(event.getChangeType());
        snapshot.setDiffPayload(buildPayload(event));
        snapshot.setNeedReview(event.isNeedReview());
        snapshot.setReviewStatus(DiffReviewStatus.PENDING.getCode());
        diffSnapshotMapper.insert(snapshot);
        log.debug("Template diff captured: variantId={}, changeType={}", event.getVariantId(), event.getChangeType());
    }

    /**
     * 将模板变更事件整理为可持久化的 JSON 结构，便于前端直接渲染。
     */
    private String buildPayload(TemplateUpdateEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(KEY_VARIANT_ID, event.getVariantId());
        payload.put(KEY_GROUP_ID, event.getGroupId());
        payload.put(KEY_VARIANT_TYPE, event.getVariantType());
        payload.put(KEY_CHANGE_TYPE, event.getChangeType());
        payload.put(KEY_CURL_TEMPLATE, event.getCurlTemplate());
        payload.put(KEY_PARAM_RULES, event.getParamRules());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw AutoTestErrorCode.DIFF_PAYLOAD_SERIALIZE_FAILED.exception(ex);
        }
    }
}
