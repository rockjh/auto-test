package com.skyler.autotest.template.listener;

import com.skyler.autotest.infra.event.SwaggerSyncEvent;
import com.skyler.autotest.template.api.enums.TemplateChangeType;
import com.skyler.autotest.template.api.enums.TemplateVariantType;
import com.skyler.autotest.template.service.TemplateGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 监听 Swagger 同步事件，自动补齐/更新模板数据。
 */
@Component
@RequiredArgsConstructor
public class SwaggerSyncEventListener {

    private static final TemplateVariantType[] DEFAULT_VARIANTS = {
            TemplateVariantType.MINIMAL,
            TemplateVariantType.FULL
    };

    private final TemplateGenerationService templateGenerationService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onSwaggerSynced(SwaggerSyncEvent event) {
        if (event.getAddedGroupIds() != null) {
            event.getAddedGroupIds().forEach(groupId ->
                    generateAllVariants(groupId, false, TemplateChangeType.CREATE));
        }
        if (event.getUpdatedGroupIds() != null) {
            event.getUpdatedGroupIds().forEach(groupId ->
                    generateAllVariants(groupId, true, TemplateChangeType.SYNC_UPDATE));
        }
        if (event.getRemovedGroupIds() != null) {
            event.getRemovedGroupIds().forEach(templateGenerationService::handleGroupRemoval);
        }
    }

    private void generateAllVariants(Long groupId, boolean markNeedReview, TemplateChangeType changeType) {
        for (TemplateVariantType variantType : DEFAULT_VARIANTS) {
            templateGenerationService.generateVariant(groupId, variantType, markNeedReview, changeType);
        }
    }
}
