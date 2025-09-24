package com.example.autotest.executor.controller;

import com.example.autotest.executor.api.ExecutionService;
import com.example.autotest.executor.api.dto.ExecutionResponse;
import com.example.autotest.executor.api.dto.ExecutionTriggerRequest;
import com.example.autotest.infra.core.web.CommonResult;
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
        return CommonResult.success(executionService.trigger(1L, request));
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
}
