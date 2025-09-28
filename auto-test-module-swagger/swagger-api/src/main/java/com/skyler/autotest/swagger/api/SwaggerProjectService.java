package com.skyler.autotest.swagger.api;

import com.skyler.autotest.swagger.api.dto.ProjectCreateRequest;
import com.skyler.autotest.swagger.api.dto.ProjectResponse;
import com.skyler.autotest.swagger.api.dto.ProjectSyncRequest;
import com.skyler.autotest.swagger.api.dto.SwaggerDiffResponse;

import java.util.List;

public interface SwaggerProjectService {

    /**
     * 创建 Swagger 项目并持久化基础配置。
     *
     * @param tenantId 当前租户编号
     * @param request  创建请求
     * @return 新项目编号
     */
    Long createProject(Long tenantId, ProjectCreateRequest request);

    /**
     * 根据项目编号获取详情。
     *
     * @param id 项目编号
     * @return 项目信息
     */
    ProjectResponse getProject(Long id);

    /**
     * 查询当前租户下的全部 Swagger 项目。
     *
     * @return 项目集合
     */
    List<ProjectResponse> listProjects();

    /**
     * 触发项目的 Swagger 同步。
     *
     * @param projectId 项目编号
     * @param request   同步请求参数
     * @return 差异摘要
     */
    SwaggerDiffResponse syncProject(Long projectId, ProjectSyncRequest request);

    /**
     * 查询项目最近一次同步的差异信息。
     *
     * @param projectId 项目编号
     * @return 差异摘要
     */
    SwaggerDiffResponse getLastDiff(Long projectId);
}
