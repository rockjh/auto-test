package com.skyler.autotest.scenario.controller;

import com.skyler.autotest.infra.core.security.SecurityContextUtils;
import com.skyler.autotest.infra.core.web.CommonResult;
import com.skyler.autotest.scenario.api.ScenarioService;
import com.skyler.autotest.scenario.api.dto.ScenarioCreateRequest;
import com.skyler.autotest.scenario.api.dto.ScenarioPublishRequest;
import com.skyler.autotest.scenario.api.dto.ScenarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 场景编排接口，覆盖创建、更新、发布和查询能力。
 */
@RestController
@RequestMapping("/api/scenario")
@RequiredArgsConstructor
@Tag(name = "场景管理", description = "场景建模与发布接口")
public class ScenarioController {

    private final ScenarioService scenarioService;

    /**
     * 创建新的自动化场景。
     *
     * @param request 场景定义
     * @return 场景编号
     */
    @PostMapping
    @Operation(summary = "创建场景", description = "根据模板步骤定义一个新的测试场景")
    public CommonResult<Long> create(@Valid @RequestBody ScenarioCreateRequest request) {
        Long tenantId = SecurityContextUtils.getRequiredTenantId();
        return CommonResult.success(scenarioService.createScenario(tenantId, request));
    }

    /**
     * 更新场景草稿信息。
     *
     * @param id      场景编号
     * @param request 场景最新定义
     * @return 空响应
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新场景", description = "覆盖场景草稿信息，未发布版本仍可修改")
    public CommonResult<Void> update(@Parameter(description = "场景编号", example = "5001001")
                                     @PathVariable Long id,
                                     @Valid @RequestBody ScenarioCreateRequest request) {
        scenarioService.updateScenario(id, request);
        return CommonResult.success(null);
    }

    /**
     * 发布场景，冻结当前步骤为新版本。
     *
     * @param id      场景编号
     * @param request 发布备注
     * @return 空响应
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布场景", description = "生成新的场景版本并标记可执行")
    public CommonResult<Void> publish(@Parameter(description = "场景编号", example = "5001001")
                                      @PathVariable Long id,
                                      @RequestBody(required = false) ScenarioPublishRequest request) {
        scenarioService.publishScenario(id, request);
        return CommonResult.success(null);
    }

    /**
     * 查询场景详情。
     *
     * @param id 场景编号
     * @return 场景详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询场景详情", description = "返回场景基础信息及最新步骤列表")
    public CommonResult<ScenarioResponse> detail(@Parameter(description = "场景编号", example = "5001001")
                                                 @PathVariable Long id) {
        return CommonResult.success(scenarioService.getScenario(id));
    }

    /**
     * 按项目查询场景列表。
     *
     * @param projectId 项目编号
     * @return 场景列表
     */
    @GetMapping
    @Operation(summary = "查询项目下场景", description = "根据项目编号返回场景草稿与已发布状态")
    public CommonResult<List<ScenarioResponse>> list(@Parameter(description = "项目编号", example = "1001")
                                                     @RequestParam Long projectId) {
        return CommonResult.success(scenarioService.listByProject(projectId));
    }
}
