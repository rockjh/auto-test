package com.skyler.autotest.swagger.controller;

import com.skyler.autotest.infra.core.context.TenantContextHolder;
import com.skyler.autotest.infra.core.web.CommonResult;
import com.skyler.autotest.swagger.api.ProjectEnvironmentService;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentCreateRequest;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentResponse;
import com.skyler.autotest.swagger.api.dto.ProjectEnvironmentUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Swagger 项目环境管理接口，提供 CRUD 与默认环境设置能力。
 */
@RestController
@RequestMapping("/api/swagger/projects/{projectId}/envs")
@RequiredArgsConstructor
@Tag(name = "Swagger 项目环境", description = "项目 Host/Header 环境维护接口")
public class ProjectEnvironmentController {

    private final ProjectEnvironmentService environmentService;

    @PostMapping
    @Operation(summary = "新增项目环境", description = "创建项目环境并可选设置为默认环境")
    public CommonResult<Long> create(@Parameter(description = "项目编号", example = "1001")
                                     @PathVariable Long projectId,
                                     @Valid @RequestBody ProjectEnvironmentCreateRequest request) {
        request.setProjectId(projectId);
        Long id = environmentService.createEnvironment(TenantContextHolder.getRequiredTenantId(), request);
        return CommonResult.success(id);
    }

    @PutMapping("/{envId}")
    @Operation(summary = "更新项目环境", description = "维护 Host/Header/变量并同步默认环境设置")
    public CommonResult<Boolean> update(@Parameter(description = "项目编号", example = "1001")
                                        @PathVariable Long projectId,
                                        @Parameter(description = "环境编号", example = "2001")
                                        @PathVariable Long envId,
                                        @Valid @RequestBody ProjectEnvironmentUpdateRequest request) {
        request.setProjectId(projectId);
        environmentService.updateEnvironment(TenantContextHolder.getRequiredTenantId(), envId, request);
        return CommonResult.success(Boolean.TRUE);
    }

    @DeleteMapping("/{envId}")
    @Operation(summary = "删除项目环境", description = "逻辑删除指定环境记录")
    public CommonResult<Boolean> delete(@Parameter(description = "环境编号", example = "2001")
                                        @PathVariable Long envId) {
        environmentService.deleteEnvironment(TenantContextHolder.getRequiredTenantId(), envId);
        return CommonResult.success(Boolean.TRUE);
    }

    @GetMapping
    @Operation(summary = "查询项目环境列表", description = "按项目返回全部环境，默认环境排在前面")
    public CommonResult<List<ProjectEnvironmentResponse>> list(@Parameter(description = "项目编号", example = "1001")
                                                               @PathVariable Long projectId) {
        return CommonResult.success(environmentService.listEnvironments(projectId));
    }

    @GetMapping("/{envId}")
    @Operation(summary = "查询环境详情", description = "返回单个环境的 Host/Header/变量配置")
    public CommonResult<ProjectEnvironmentResponse> detail(@Parameter(description = "环境编号", example = "2001")
                                                           @PathVariable Long envId) {
        return CommonResult.success(environmentService.getEnvironment(envId));
    }
}
