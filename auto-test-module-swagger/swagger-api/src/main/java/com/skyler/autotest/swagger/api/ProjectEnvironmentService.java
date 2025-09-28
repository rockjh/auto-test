package com.skyler.autotest.swagger.api;

import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentCreateRequest;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentResponse;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentUpdateRequest;

import java.util.List;

/**
 * Swagger 项目环境相关业务接口。
 */
public interface ProjectEnvironmentService {

    /**
     * 创建项目环境。
     *
     * @param tenantId 当前租户编号
     * @param request  创建请求
     * @return 环境编号
     */
    Long createEnvironment(Long tenantId, ProjectEnvironmentCreateRequest request);

    /**
     * 更新项目环境。
     *
     * @param tenantId      当前租户编号
     * @param environmentId 环境编号
     * @param request       更新请求
     */
    void updateEnvironment(Long tenantId, Long environmentId, ProjectEnvironmentUpdateRequest request);

    /**
     * 删除项目环境。
     *
     * @param tenantId      当前租户编号
     * @param environmentId 环境编号
     */
    void deleteEnvironment(Long tenantId, Long environmentId);

    /**
     * 获取环境详情。
     *
     * @param environmentId 环境编号
     * @return 环境详情
     */
    ProjectEnvironmentResponse getEnvironment(Long environmentId);

    /**
     * 查询项目下的全部环境。
     *
     * @param projectId 项目编号
     * @return 环境列表
     */
    List<ProjectEnvironmentResponse> listEnvironments(Long projectId);
}
