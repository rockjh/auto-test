package com.example.autotest.swagger.controller;

import com.example.autotest.infra.core.web.CommonResult;
import com.example.autotest.swagger.api.SwaggerProjectService;
import com.example.autotest.swagger.api.dto.ProjectCreateRequest;
import com.example.autotest.swagger.api.dto.ProjectResponse;
import com.example.autotest.swagger.api.dto.ProjectSyncRequest;
import com.example.autotest.swagger.api.dto.SwaggerDiffResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Swagger 项目管理接口，对应文档《Swagger 同步》流程。
 */
@RestController
@RequestMapping("/api/swagger/projects")
@RequiredArgsConstructor
@Tag(name = "Swagger 项目", description = "Swagger 项目同步与差异查询接口")
public class SwaggerProjectController {

    private final SwaggerProjectService projectService;

    /**
     * 创建新的 Swagger 项目。
     *
     * @param request 创建参数
     * @return 新建项目的编号
     */
    @PostMapping
    @Operation(summary = "创建 Swagger 项目", description = "根据 Swagger 配置创建项目并触发首次入库")
    public CommonResult<Long> create(@Valid @RequestBody ProjectCreateRequest request) {
        Long id = projectService.createProject(1L, request);
        return CommonResult.success(id);
    }

    /**
     * 查询当前租户下的 Swagger 项目列表。
     *
     * @return 项目列表
     */
    @GetMapping
    @Operation(summary = "查询项目列表", description = "返回当前租户下所有 Swagger 项目的同步信息")
    public CommonResult<List<ProjectResponse>> list() {
        return CommonResult.success(projectService.listProjects());
    }

    /**
     * 查询指定项目详情。
     *
     * @param id 项目编号
     * @return 项目详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询项目详情", description = "根据项目编号返回最新的 Swagger 元数据")
    public CommonResult<ProjectResponse> detail(@Parameter(description = "项目编号", example = "1001")
                                                @PathVariable Long id) {
        return CommonResult.success(projectService.getProject(id));
    }

    /**
     * 手动触发项目的 Swagger 同步。
     *
     * @param id      项目编号
     * @param request 同步参数
     * @return 同步结果概要
     */
    @PostMapping("/{id}/sync")
    @Operation(summary = "触发 Swagger 同步", description = "手动重新拉取 Swagger 文档并记录差异")
    public CommonResult<SwaggerDiffResponse> sync(@Parameter(description = "项目编号", example = "1001")
                                                  @PathVariable Long id,
                                                  @RequestBody ProjectSyncRequest request) {
        return CommonResult.success(projectService.syncProject(id, request));
    }

    /**
     * 查询项目最近一次同步的差异情况。
     *
     * @param id 项目编号
     * @return 差异摘要
     */
    @GetMapping("/{id}/diff")
    @Operation(summary = "查询最新差异", description = "查看项目最近一次同步的差异摘要")
    public CommonResult<SwaggerDiffResponse> lastDiff(@Parameter(description = "项目编号", example = "1001")
                                                      @PathVariable Long id) {
        return CommonResult.success(projectService.getLastDiff(id));
    }
}
