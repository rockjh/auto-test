package com.skyler.autotest.infra.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Swagger 同步完成后发布的事件，用于驱动模板生成、差异快照等后续流程。
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "Swagger 同步事件载荷")
public class SwaggerSyncEvent {

    @Schema(description = "租户编号")
    private final Long tenantId;

    @Schema(description = "项目编号")
    private final Long projectId;

    @Schema(description = "同步记录主键")
    private final Long syncId;

    @Schema(description = "同步产生的差异摘要 JSON")
    private final String diffSummary;

    @Schema(description = "本次新增接口对应的 groupId 列表")
    private final List<Long> addedGroupIds;

    @Schema(description = "本次结构变更接口对应的 groupId 列表")
    private final List<Long> updatedGroupIds;

    @Schema(description = "本次下线接口对应的 groupId 列表")
    private final List<Long> removedGroupIds;
}
