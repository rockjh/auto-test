package com.skyler.autotest.executor.controller;

import com.skyler.autotest.executor.api.ExecutionService;
import com.skyler.autotest.executor.api.dto.ExecutionLogResponse;
import com.skyler.autotest.executor.api.dto.ExecutionResponse;
import com.skyler.autotest.executor.api.dto.ExecutionTriggerRequest;
import com.skyler.autotest.infra.core.page.PageResult;
import com.skyler.autotest.infra.core.security.SecurityContextUtils;
import com.skyler.autotest.infra.core.web.CommonResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 执行器接口，提供场景触发与执行详情查询。
 */
@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
@Tag(name = "执行器", description = "场景执行触发与结果查询接口")
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * 触发场景执行。
     *
     * @param request 触发参数
     * @return 执行记录编号
     */
    @PostMapping("/trigger")
    @Operation(summary = "触发执行", description = "串行执行场景步骤并生成执行记录")
    public CommonResult<Long> trigger(@Valid @RequestBody ExecutionTriggerRequest request) {
        Long tenantId = SecurityContextUtils.getRequiredTenantId();
        return CommonResult.success(executionService.trigger(tenantId, request));
    }

    /**
     * 查询执行详情。
     *
     * @param id 执行记录编号
     * @return 执行详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询执行详情", description = "包含步骤执行状态与请求响应快照")
    public CommonResult<ExecutionResponse> detail(@Parameter(description = "执行记录编号", example = "9001001")
                                                  @PathVariable Long id) {
        return CommonResult.success(executionService.getDetail(id));
    }

    /**
     * 分页查询执行日志。
     *
     * @param id 执行记录编号
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 执行日志分页结果
     */
    @GetMapping("/{id}/logs")
    @Operation(summary = "分页查询执行日志", description = "按执行记录编号分页返回执行日志")
    public CommonResult<PageResult<ExecutionLogResponse>> logs(
            @Parameter(description = "执行记录编号", example = "9001001")
            @PathVariable Long id,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(executionService.pageLogs(id, pageNo, pageSize));
    }
}
