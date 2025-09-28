package com.skyler.autotest.swagger.service;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.swagger.api.ProjectEnvironmentService;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentCreateRequest;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentResponse;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentUpdateRequest;
import com.skyler.autotest.swagger.dal.dataobject.ProjectDO;
import com.skyler.autotest.swagger.dal.dataobject.ProjectEnvironmentDO;
import com.skyler.autotest.swagger.dal.mapper.ProjectEnvironmentMapper;
import com.skyler.autotest.swagger.dal.mapper.ProjectMapper;
import com.skyler.autotest.swagger.enums.ProjectEnvironmentTypeEnum;
import com.skyler.autotest.swagger.enums.SwaggerErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Swagger 项目环境领域服务实现。
 */
@Service
@RequiredArgsConstructor
public class ProjectEnvironmentServiceImpl implements ProjectEnvironmentService {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ProjectEnvironmentMapper environmentMapper;
    private final ProjectMapper projectMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEnvironment(Long tenantId, ProjectEnvironmentCreateRequest request) {
        ProjectDO project = requireProject(request.getProjectId());
        assertSameTenant(tenantId, project.getTenantId());

        ProjectEnvironmentDO environment = new ProjectEnvironmentDO();
        environment.setId(bizIdGenerator.nextId());
        environment.setTenantId(project.getTenantId());
        environment.setProjectId(project.getId());
        environment.setName(request.getName());
        environment.setEnvType(ProjectEnvironmentTypeEnum.resolve(request.getEnvType()).getCode());
        environment.setHost(StringUtils.trim(request.getHost()));
        environment.setHeaders(writeJson(request.getHeaders()));
        environment.setVariables(writeJson(request.getVariables()));
        environment.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        environment.setStatus(request.getStatus() != null ? request.getStatus() : CommonStatusEnum.ENABLE.getStatus());
        environment.setRemark(request.getRemark());
        environment.setDeleted(Boolean.FALSE);
        environmentMapper.insert(environment);

        if (Boolean.TRUE.equals(environment.getIsDefault())) {
            resetOtherDefaults(environment);
        }
        return environment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnvironment(Long tenantId, Long environmentId, ProjectEnvironmentUpdateRequest request) {
        ProjectEnvironmentDO environment = requireEnvironment(environmentId);
        assertSameTenant(tenantId, environment.getTenantId());
        if (!environment.getProjectId().equals(request.getProjectId())) {
            throw new BizException(SwaggerErrorCode.ENVIRONMENT_PROJECT_MISMATCH);
        }

        environment.setName(request.getName());
        if (request.getEnvType() != null) {
            environment.setEnvType(ProjectEnvironmentTypeEnum.resolve(request.getEnvType()).getCode());
        }
        environment.setHost(StringUtils.trim(request.getHost()));
        if (request.getHeaders() != null) {
            environment.setHeaders(writeJson(request.getHeaders()));
        }
        if (request.getVariables() != null) {
            environment.setVariables(writeJson(request.getVariables()));
        }
        if (request.getIsDefault() != null) {
            environment.setIsDefault(request.getIsDefault());
        }
        if (request.getStatus() != null) {
            environment.setStatus(request.getStatus());
        }
        environment.setRemark(request.getRemark());
        environment.setUpdateTime(LocalDateTime.now());
        environmentMapper.updateById(environment);

        if (Boolean.TRUE.equals(environment.getIsDefault())) {
            resetOtherDefaults(environment);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEnvironment(Long tenantId, Long environmentId) {
        ProjectEnvironmentDO environment = requireEnvironment(environmentId);
        assertSameTenant(tenantId, environment.getTenantId());
        environmentMapper.deleteById(environmentId);
    }

    @Override
    public ProjectEnvironmentResponse getEnvironment(Long environmentId) {
        ProjectEnvironmentDO environment = environmentMapper.selectById(environmentId);
        return environment == null ? null : toResponse(environment);
    }

    @Override
    public List<ProjectEnvironmentResponse> listEnvironments(Long projectId) {
        List<ProjectEnvironmentDO> environments = environmentMapper.selectList(new LambdaQueryWrapper<ProjectEnvironmentDO>()
                .eq(ProjectEnvironmentDO::getProjectId, projectId)
                .orderByDesc(ProjectEnvironmentDO::getIsDefault)
                .orderByAsc(ProjectEnvironmentDO::getCreateTime));
        return environments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ProjectEnvironmentDO requireEnvironment(Long environmentId) {
        ProjectEnvironmentDO environment = environmentMapper.selectById(environmentId);
        if (environment == null) {
            throw new BizException(SwaggerErrorCode.ENVIRONMENT_NOT_FOUND);
        }
        return environment;
    }

    private ProjectDO requireProject(Long projectId) {
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(SwaggerErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private void assertSameTenant(Long operatorTenantId, Long dataTenantId) {
        if (operatorTenantId != null && !operatorTenantId.equals(dataTenantId)) {
            throw new BizException(SwaggerErrorCode.TENANT_FORBIDDEN);
        }
    }

    private void resetOtherDefaults(ProjectEnvironmentDO environment) {
        LambdaUpdateWrapper<ProjectEnvironmentDO> wrapper = new LambdaUpdateWrapper<ProjectEnvironmentDO>()
                .eq(ProjectEnvironmentDO::getProjectId, environment.getProjectId())
                .ne(ProjectEnvironmentDO::getId, environment.getId())
                .set(ProjectEnvironmentDO::getIsDefault, false);
        environmentMapper.update(null, wrapper);
    }

    private ProjectEnvironmentResponse toResponse(ProjectEnvironmentDO environment) {
        ProjectEnvironmentResponse response = new ProjectEnvironmentResponse();
        response.setId(environment.getId());
        response.setProjectId(environment.getProjectId());
        response.setName(environment.getName());
        response.setEnvType(environment.getEnvType());
        response.setHost(environment.getHost());
        response.setHeaders(parseHeaders(environment.getHeaders()));
        response.setVariables(parseVariables(environment.getVariables()));
        response.setIsDefault(environment.getIsDefault());
        response.setStatus(environment.getStatus());
        response.setRemark(environment.getRemark());
        response.setCreateTime(environment.getCreateTime());
        response.setUpdateTime(environment.getUpdateTime());
        return response;
    }

    private Map<String, String> parseHeaders(String content) {
        if (StringUtils.isBlank(content)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(content, STRING_MAP);
        } catch (Exception ex) {
            throw new BizException(SwaggerErrorCode.ENVIRONMENT_HEADER_JSON_INVALID);
        }
    }

    private Map<String, Object> parseVariables(String content) {
        if (StringUtils.isBlank(content)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(content, OBJECT_MAP);
        } catch (Exception ex) {
            throw new BizException(SwaggerErrorCode.ENVIRONMENT_VARIABLE_JSON_INVALID);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BizException(SwaggerErrorCode.ENVIRONMENT_JSON_SERIALIZE_FAILED, ex);
        }
    }
}
