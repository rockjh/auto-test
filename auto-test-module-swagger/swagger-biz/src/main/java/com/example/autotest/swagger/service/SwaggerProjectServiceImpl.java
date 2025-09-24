package com.example.autotest.swagger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.autotest.infra.core.exception.BizException;
import com.example.autotest.infra.core.id.BizIdGenerator;
import com.example.autotest.infra.core.util.JsonUtils;
import com.example.autotest.infra.event.SwaggerSyncEvent;
import com.example.autotest.swagger.api.SwaggerProjectService;
import com.example.autotest.swagger.api.dto.ProjectCreateRequest;
import com.example.autotest.swagger.api.dto.ProjectResponse;
import com.example.autotest.swagger.api.dto.ProjectSyncRequest;
import com.example.autotest.swagger.api.dto.SwaggerDiffResponse;
import com.example.autotest.swagger.converter.SwaggerProjectConverter;
import com.example.autotest.swagger.dal.dataobject.CollectionDO;
import com.example.autotest.swagger.dal.dataobject.GroupDO;
import com.example.autotest.swagger.dal.dataobject.ProjectDO;
import com.example.autotest.swagger.dal.dataobject.SwaggerSyncDO;
import com.example.autotest.swagger.dal.mapper.CollectionMapper;
import com.example.autotest.swagger.dal.mapper.GroupMapper;
import com.example.autotest.swagger.dal.mapper.ProjectMapper;
import com.example.autotest.swagger.dal.mapper.SwaggerSyncMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swagger 项目管理与同步核心实现，负责落地接口元数据并触发后续事件链。
 */
@Service
@RequiredArgsConstructor
public class SwaggerProjectServiceImpl implements SwaggerProjectService {

    private final ProjectMapper projectMapper;
    private final CollectionMapper collectionMapper;
    private final GroupMapper groupMapper;
    private final SwaggerSyncMapper swaggerSyncMapper;
    private final BizIdGenerator bizIdGenerator;
    private final JsonUtils jsonUtils;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Long createProject(Long tenantId, ProjectCreateRequest request) {
        ProjectDO project = new ProjectDO();
        project.setId(bizIdGenerator.nextId());
        project.setTenantId(tenantId);
        project.setName(request.getName());
        project.setSwaggerSource(request.getSwaggerSource());
        project.setSwaggerType(request.getSwaggerType());
        project.setSyncStatus(0);
        project.setStatus(1);
        projectMapper.insert(project);
        return project.getId();
    }

    @Override
    public ProjectResponse getProject(Long id) {
        ProjectDO projectDO = projectMapper.selectById(id);
        return SwaggerProjectConverter.toResponse(projectDO);
    }

    @Override
    public List<ProjectResponse> listProjects() {
        List<ProjectDO> list = projectMapper.selectList(new LambdaQueryWrapper<>());
        return list.stream().map(SwaggerProjectConverter::toResponse).toList();
    }

    /**
     * 执行 Swagger 同步流程：解析文档、比对差异、写库并发布同步事件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwaggerDiffResponse syncProject(Long projectId, ProjectSyncRequest request) {
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(404, "project not found");
        }

        SwaggerSyncDO sync = new SwaggerSyncDO();
        sync.setId(bizIdGenerator.nextId());
        sync.setProjectId(projectId);
        sync.setTenantId(project.getTenantId());
        sync.setTriggerType(Optional.ofNullable(request.getTriggerType()).orElse(1));
        sync.setStatus(0);
        sync.setStartTime(LocalDateTime.now());
        swaggerSyncMapper.insert(sync);

        try {
            List<Long> addedGroupIds = new ArrayList<>();
            List<Long> updatedGroupIds = new ArrayList<>();
            String swaggerContent = resolveSwaggerContent(project, request);
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(swaggerContent, null, null).getOpenAPI();
            if (openAPI == null) {
                throw new BizException(500, "failed to parse swagger, please check format");
            }

            String swaggerJson = objectMapper.writeValueAsString(openAPI);
            String swaggerHash = DigestUtils.sha256Hex(swaggerJson);

            Map<String, GroupDO> existingGroupMap = new ConcurrentHashMap<>();
            groupMapper.selectList(new LambdaQueryWrapper<GroupDO>().eq(GroupDO::getProjectId, projectId))
                    .forEach(group -> existingGroupMap.put(buildGroupKey(group.getMethod(), group.getPath()), group));

            Map<String, CollectionDO> tagCollectionMap = new HashMap<>();
            collectionMapper.selectList(new LambdaQueryWrapper<CollectionDO>().eq(CollectionDO::getProjectId, projectId))
                    .forEach(collection -> tagCollectionMap.put(collection.getTag(), collection));

            List<String> added = new ArrayList<>();
            List<String> updated = new ArrayList<>();
            Paths paths = openAPI.getPaths();
            if (paths != null) {
                paths.forEach((path, pathItem) -> processPathItem(project, sync, existingGroupMap,
                        tagCollectionMap, added, updated, addedGroupIds, updatedGroupIds, path, pathItem));
            }

            List<GroupDO> removedGroups = existingGroupMap.values().stream()
                    .filter(group -> group.getLastSyncId() == null || !group.getLastSyncId().equals(sync.getId()))
                    .toList();
            removedGroups.forEach(group -> {
                group.setStatus(0);
                groupMapper.updateById(group);
            });
            List<String> removed = removedGroups.stream()
                    .map(group -> group.getMethod() + " " + group.getPath())
                    .toList();
            List<Long> removedGroupIds = removedGroups.stream()
                    .map(GroupDO::getId)
                    .toList();

            Map<String, Object> diff = new HashMap<>();
            diff.put("added", added);
            diff.put("updated", updated);
            diff.put("removed", removed);
            sync.setDiffSummary(jsonUtils.toJson(diff));
            sync.setStatus(1);
            sync.setEndTime(LocalDateTime.now());
            swaggerSyncMapper.updateById(sync);

            project.setSwaggerVersion(openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "unknown");
            project.setSwaggerHash(swaggerHash);
            project.setSyncStatus(1);
            project.setSyncTime(LocalDateTime.now());
            projectMapper.updateById(project);

            eventPublisher.publishEvent(new SwaggerSyncEvent(
                    project.getTenantId(),
                    project.getId(),
                    sync.getId(),
                    sync.getDiffSummary(),
                    addedGroupIds,
                    updatedGroupIds,
                    removedGroupIds
            ));

            return buildDiffResponse(sync);
        } catch (Exception ex) {
            sync.setStatus(2);
            sync.setErrorMessage(ex.getMessage());
            sync.setEndTime(LocalDateTime.now());
            swaggerSyncMapper.updateById(sync);
            throw ex;
        }
    }

    /**
     * 处理单个接口路径下的所有 HTTP 操作，生成或更新对应的 Group 记录。
     */
    private void processPathItem(ProjectDO project,
                                 SwaggerSyncDO sync,
                                 Map<String, GroupDO> existingGroupMap,
                                 Map<String, CollectionDO> tagCollectionMap,
                                 List<String> added,
                                 List<String> updated,
                                 List<Long> addedGroupIds,
                                 List<Long> updatedGroupIds,
                                 String path,
                                 PathItem pathItem) {
        if (pathItem == null) {
            return;
        }
        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
        operations.forEach((httpMethod, operation) -> {
            String method = httpMethod.name();
            String key = buildGroupKey(method, path);
            GroupDO existing = existingGroupMap.get(key);
            String operationJson = safeToJson(operation);
            String operationHash = DigestUtils.sha256Hex(operationJson);

            GroupDO group = Optional.ofNullable(existing).orElseGet(() -> {
                GroupDO created = new GroupDO();
                created.setId(bizIdGenerator.nextId());
                created.setTenantId(project.getTenantId());
                created.setProjectId(project.getId());
                created.setPath(path);
                created.setMethod(method);
                created.setStatus(1);
                return created;
            });
            group.setCollectionId(resolveCollection(project, operation, tagCollectionMap));
            group.setSummary(operation.getSummary());
            group.setOperationId(operation.getOperationId());
            group.setHash(operationHash);
            group.setRequestSchema(operationJson);
            group.setResponseSchema(operation.getResponses() != null ? safeToJson(operation.getResponses()) : null);
            group.setLastSyncId(sync.getId());

            if (existing == null) {
                groupMapper.insert(group);
                existingGroupMap.put(key, group);
                added.add(method + " " + path);
                addedGroupIds.add(group.getId());
            } else {
                group.setId(existing.getId());
                group.setTenantId(existing.getTenantId());
                groupMapper.updateById(group);
                existingGroupMap.put(key, group);
                if (!StringUtils.equals(existing.getHash(), operationHash)) {
                    updated.add(method + " " + path);
                    updatedGroupIds.add(group.getId());
                }
            }
        });
    }

    private Long resolveCollection(ProjectDO project,
                                   Operation operation,
                                   Map<String, CollectionDO> tagCollectionMap) {
        String tag = Optional.ofNullable(operation.getTags())
                .filter(tags -> !tags.isEmpty())
                .map(tags -> tags.get(0))
                .orElse("default");
        CollectionDO collection = tagCollectionMap.get(tag);
        if (collection == null) {
            collection = new CollectionDO();
            collection.setId(bizIdGenerator.nextId());
            collection.setTenantId(project.getTenantId());
            collection.setProjectId(project.getId());
            collection.setTag(tag);
            collection.setSummary(tag);
            collection.setStatus(1);
            collectionMapper.insert(collection);
            tagCollectionMap.put(tag, collection);
        }
        return collection.getId();
    }

    private String safeToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("failed to serialize", e);
        }
    }

    private String resolveSwaggerContent(ProjectDO project, ProjectSyncRequest request) {
        if (StringUtils.isNotBlank(request.getOverrideSource())) {
            return request.getOverrideSource();
        }
        if (project.getSwaggerType() != null && project.getSwaggerType() == 1 && StringUtils.startsWith(project.getSwaggerSource(), "http")) {
            throw new BizException(400, "network fetch is disabled in current environment");
        }
        try {
            Path path = Path.of(project.getSwaggerSource());
            return Files.readString(path);
        } catch (IOException e) {
            throw new BizException(500, "failed to read swagger source: " + e.getMessage());
        }
    }

    private SwaggerDiffResponse buildDiffResponse(SwaggerSyncDO sync) {
        SwaggerDiffResponse response = new SwaggerDiffResponse();
        response.setSyncId(sync.getId());
        response.setStatus(sync.getStatus());
        response.setDiffSummary(sync.getDiffSummary());
        response.setStartTime(sync.getStartTime());
        response.setEndTime(sync.getEndTime());
        response.setErrorMessage(sync.getErrorMessage());
        return response;
    }

    private String buildGroupKey(String method, String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path;
    }

    @Override
    public SwaggerDiffResponse getLastDiff(Long projectId) {
        LambdaQueryWrapper<SwaggerSyncDO> wrapper = new LambdaQueryWrapper<SwaggerSyncDO>()
                .eq(SwaggerSyncDO::getProjectId, projectId)
                .orderByDesc(SwaggerSyncDO::getStartTime)
                .last("limit 1");
        SwaggerSyncDO sync = swaggerSyncMapper.selectOne(wrapper);
        return sync == null ? null : buildDiffResponse(sync);
    }
}
