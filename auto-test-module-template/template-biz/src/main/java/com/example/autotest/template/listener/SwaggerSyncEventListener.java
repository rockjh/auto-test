package com.example.autotest.template.listener;

import com.example.autotest.infra.event.SwaggerSyncEvent;
import com.example.autotest.template.service.TemplateGenerationService;
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

    private final TemplateGenerationService templateGenerationService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onSwaggerSynced(SwaggerSyncEvent event) {
        if (event.getAddedGroupIds() != null) {
            event.getAddedGroupIds().forEach(groupId ->
                    templateGenerationService.generateMinimalVariant(groupId, false, "CREATE"));
        }
        if (event.getUpdatedGroupIds() != null) {
            event.getUpdatedGroupIds().forEach(groupId ->
                    templateGenerationService.generateMinimalVariant(groupId, true, "SYNC_UPDATE"));
        }
        if (event.getRemovedGroupIds() != null) {
            event.getRemovedGroupIds().forEach(templateGenerationService::handleGroupRemoval);
        }
    }
}
