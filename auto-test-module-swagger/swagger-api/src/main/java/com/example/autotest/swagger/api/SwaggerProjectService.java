package com.example.autotest.swagger.api;

import com.example.autotest.swagger.api.dto.ProjectCreateRequest;
import com.example.autotest.swagger.api.dto.ProjectResponse;
import com.example.autotest.swagger.api.dto.ProjectSyncRequest;
import com.example.autotest.swagger.api.dto.SwaggerDiffResponse;

import java.util.List;

public interface SwaggerProjectService {

    Long createProject(Long tenantId, ProjectCreateRequest request);

    ProjectResponse getProject(Long id);

    List<ProjectResponse> listProjects();

    SwaggerDiffResponse syncProject(Long projectId, ProjectSyncRequest request);

    SwaggerDiffResponse getLastDiff(Long projectId);
}
