package com.skyler.autotest.executor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skyler.autotest.executor.api.ExecutionService;
import com.skyler.autotest.executor.api.dto.ExecutionLogResponse;
import com.skyler.autotest.executor.api.dto.ExecutionResponse;
import com.skyler.autotest.executor.api.dto.ExecutionTriggerRequest;
import com.skyler.autotest.executor.converter.ExecutionConverter;
import com.skyler.autotest.executor.dal.dataobject.ExecutionDO;
import com.skyler.autotest.executor.dal.dataobject.ExecutionDetailDO;
import com.skyler.autotest.executor.dal.mapper.ExecutionDetailMapper;
import com.skyler.autotest.executor.dal.mapper.ExecutionMapper;
import com.skyler.autotest.executor.enums.ExecutionStatus;
import com.skyler.autotest.executor.enums.ExecutionStepStatus;
import com.skyler.autotest.executor.error.ExecutorErrorCode;
import com.skyler.autotest.executor.lock.ScenarioLockRegistry;
import com.skyler.autotest.executor.runner.StepRunner;
import com.skyler.autotest.executor.runner.VariableContext;
import com.skyler.autotest.executor.service.ExecutionLogService;
import com.skyler.autotest.executor.service.dto.ExecutionLogCreateRequest;
import com.skyler.autotest.infra.core.error.EnvironmentErrorCode;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.infra.core.page.PageResult;
import com.skyler.autotest.infra.event.ScenarioExecutionEvent;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioDO;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioStepDO;
import com.skyler.autotest.scenario.dal.mapper.ScenarioMapper;
import com.skyler.autotest.scenario.dal.mapper.ScenarioStepMapper;
import com.skyler.autotest.scenario.error.ScenarioErrorCode;
import com.skyler.autotest.swagger.dal.dataobject.ProjectEnvironmentDO;
import com.skyler.autotest.swagger.dal.mapper.ProjectEnvironmentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行器对外服务实现，按顺序执行场景步骤并记录执行结果。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionServiceImpl implements ExecutionService {

    private static final TypeReference<Map<String, String>> HEADER_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> VARIABLE_TYPE = new TypeReference<>() {
    };
    private static final int ENVIRONMENT_STATUS_ENABLED = 1;

    private final ExecutionMapper executionMapper;
    private final ExecutionDetailMapper executionDetailMapper;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final ExecutionLogService executionLogService;
    private final ProjectEnvironmentMapper projectEnvironmentMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ScenarioLockRegistry scenarioLockRegistry;
    private final StepRunner stepRunner;

    /**
     * 触发场景执行，串行处理步骤并产生日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long trigger(Long tenantId, ExecutionTriggerRequest request) {
        ScenarioDO scenario = scenarioMapper.selectById(request.getScenarioId());
        if (scenario == null) {
            throw new BizException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
        }
        List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getScenarioId, scenario.getId())
                .orderByAsc(ScenarioStepDO::getOrderNo));
        if (steps.isEmpty()) {
            throw new BizException(ExecutorErrorCode.SCENARIO_STEPS_EMPTY);
        }

        Long targetEnvId = request.getEnvId() != null ? request.getEnvId() : scenario.getDefaultEnvId();
        ProjectEnvironmentDO environment = resolveExecutionEnvironment(tenantId, scenario, targetEnvId);

        tryLockScenario(scenario.getId());
        ExecutionDO execution = createExecution(tenantId, request, scenario, environment);
        VariableContext variableContext = new VariableContext(objectMapper);
        variableContext.put("host", environment.getHost());
        mergeEnvironmentVariables(variableContext, environment);

        recordExecutionLog(tenantId, scenario.getProjectId(), execution.getId(), null,
                "INFO", "场景执行开始", Map.of(
                        "scenarioId", scenario.getId(),
                        "stepCount", steps.size(),
                        "envId", environment.getId()
                ));

        boolean success = true;
        String failureMessage = null;
        Throwable executionThrowable = null;

        try {
            int stepOrder = 1;
            for (ScenarioStepDO step : steps) {
                ExecutionDetailDO detail = initExecutionDetail(tenantId, execution, step, stepOrder++);
                StepRunner.StepRunContext context = new StepRunner.StepRunContext(
                        tenantId,
                        scenario.getProjectId(),
                        execution.getId(),
                        detail.getStepOrder(),
                        step,
                        variableContext
                );
                StepRunner.StepRunResult result = stepRunner.run(context);

                detail.setRequestSnapshot(result.getRequestSnapshot());
                detail.setResponseSnapshot(result.getResponseSnapshot());
                detail.setVariablesSnapshot(result.getVariablesSnapshot());
                detail.setStatus(result.isSuccess()
                        ? ExecutionStepStatus.SUCCESS.getCode()
                        : ExecutionStepStatus.FAILED.getCode());
                detail.setErrorMessage(result.isSuccess() ? null : truncateMessage(result.getErrorMessage()));
                detail.setEndTime(LocalDateTime.now());
                executionDetailMapper.updateById(detail);

                if (!result.isSuccess()) {
                    success = false;
                    failureMessage = result.getErrorMessage();
                    break;
                }
            }
        } catch (Throwable ex) {
            success = false;
            failureMessage = StringUtils.defaultIfBlank(failureMessage, ex.getMessage());
            executionThrowable = ex;
            log.error("execution failed", ex);
        } finally {
            finalizeExecution(execution, success, failureMessage);
            scenarioLockRegistry.unlock(scenario.getId());
            eventPublisher.publishEvent(new ScenarioExecutionEvent(
                    tenantId,
                    execution.getId(),
                    execution.getScenarioId(),
                    execution.getStatus(),
                    execution.getDurationMs()
            ));
        }

        if (!success) {
            if (executionThrowable instanceof RuntimeException runtime) {
                throw runtime;
            }
            String message = StringUtils.defaultIfBlank(failureMessage, ExecutorErrorCode.EXECUTION_FAILED.getMessage());
            if (executionThrowable != null) {
                throw new BizException(ExecutorErrorCode.EXECUTION_FAILED, message, executionThrowable);
            }
            throw new BizException(ExecutorErrorCode.EXECUTION_FAILED, message);
        }
        return execution.getId();
    }

    /**
     * 查询执行详情及步骤列表。
     */
    @Override
    public ExecutionResponse getDetail(Long executionId) {
        ExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return null;
        }
        List<ExecutionDetailDO> details = executionDetailMapper.selectList(new LambdaQueryWrapper<ExecutionDetailDO>()
                .eq(ExecutionDetailDO::getExecutionId, executionId)
                .orderByAsc(ExecutionDetailDO::getStepOrder));
        return ExecutionConverter.toResponse(execution, details);
    }

    /**
     * 分页查询执行日志。
     */
    @Override
    public PageResult<ExecutionLogResponse> pageLogs(Long executionId, int pageNo, int pageSize) {
        ExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return PageResult.empty();
        }
        return executionLogService.pageLogs(execution.getProjectId(), executionId, pageNo, pageSize);
    }

    private ExecutionDO createExecution(Long tenantId,
                                        ExecutionTriggerRequest request,
                                        ScenarioDO scenario,
                                        ProjectEnvironmentDO environment) {
        ExecutionDO execution = new ExecutionDO();
        execution.setId(bizIdGenerator.nextId());
        execution.setTenantId(tenantId);
        execution.setScenarioId(scenario.getId());
        execution.setProjectId(scenario.getProjectId());
        execution.setEnvId(environment.getId());
        execution.setTriggerType("manual");
        execution.setTriggerUser("system");
        execution.setStatus(ExecutionStatus.RUNNING.getCode());
        execution.setStartTime(LocalDateTime.now());
        execution.setRemark(request.getRemark());
        executionMapper.insert(execution);
        return execution;
    }

    private ExecutionDetailDO initExecutionDetail(Long tenantId,
                                                   ExecutionDO execution,
                                                   ScenarioStepDO step,
                                                   int stepOrder) {
        ExecutionDetailDO detail = new ExecutionDetailDO();
        detail.setId(bizIdGenerator.nextId());
        detail.setTenantId(tenantId);
        detail.setExecutionId(execution.getId());
        detail.setScenarioStepId(step.getId());
        detail.setStepOrder(stepOrder);
        detail.setStatus(ExecutionStepStatus.RUNNING.getCode());
        detail.setStartTime(LocalDateTime.now());
        executionDetailMapper.insert(detail);
        return detail;
    }

    private void finalizeExecution(ExecutionDO execution, boolean success, String failureMessage) {
        execution.setEndTime(LocalDateTime.now());
        execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));
        if (success) {
            execution.setStatus(ExecutionStatus.SUCCESS.getCode());
            execution.setSummary(null);
            executionMapper.updateById(execution);
            recordExecutionLog(execution.getTenantId(), execution.getProjectId(), execution.getId(), null,
                    "INFO", "场景执行完成", Map.of(
                            "status", execution.getStatus(),
                            "durationMs", execution.getDurationMs()
                    ));
        } else {
            execution.setStatus(ExecutionStatus.FAILED.getCode());
            execution.setSummary(truncateMessage(failureMessage));
            executionMapper.updateById(execution);
            recordExecutionLog(execution.getTenantId(), execution.getProjectId(), execution.getId(), null,
                    "ERROR", "场景执行失败", Map.of(
                            "status", execution.getStatus(),
                            "error", truncateMessage(failureMessage)
                    ));
        }
    }

    private void tryLockScenario(Long scenarioId) {
        try {
            if (!scenarioLockRegistry.tryLock(scenarioId, 2000)) {
                throw new BizException(ExecutorErrorCode.SCENARIO_LOCKED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ExecutorErrorCode.SCENARIO_LOCK_INTERRUPTED, e);
        }
    }

    private ProjectEnvironmentDO resolveExecutionEnvironment(Long tenantId,
                                                             ScenarioDO scenario,
                                                             Long envId) {
        if (envId == null) {
            throw new BizException(ExecutorErrorCode.ENVIRONMENT_REQUIRED);
        }
        ProjectEnvironmentDO environment = projectEnvironmentMapper.selectById(envId);
        if (environment == null || Boolean.TRUE.equals(environment.getDeleted())) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_NOT_FOUND);
        }
        if (!scenario.getProjectId().equals(environment.getProjectId())) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_PROJECT_MISMATCH);
        }
        if (tenantId != null && environment.getTenantId() != null && !tenantId.equals(environment.getTenantId())) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_TENANT_DENIED);
        }
        if (environment.getStatus() != null && !environment.getStatus().equals(ENVIRONMENT_STATUS_ENABLED)) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_DISABLED);
        }
        if (StringUtils.isBlank(environment.getHost())) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_HOST_EMPTY);
        }
        return environment;
    }

    private void mergeEnvironmentVariables(VariableContext variableContext,
                                            ProjectEnvironmentDO environment) {
        Map<String, String> headers = parseEnvironmentHeaders(environment.getHeaders());
        headers.forEach((key, value) -> variableContext.put("header." + key, value));
        Map<String, Object> envVariables = parseEnvironmentVariables(environment.getVariables());
        envVariables.forEach(variableContext::put);
    }

    private Map<String, String> parseEnvironmentHeaders(String headers) {
        if (StringUtils.isBlank(headers)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(headers, HEADER_TYPE);
        } catch (JsonProcessingException e) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_HEADER_INVALID, e);
        }
    }

    private Map<String, Object> parseEnvironmentVariables(String variables) {
        if (StringUtils.isBlank(variables)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(variables, VARIABLE_TYPE);
        } catch (JsonProcessingException e) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_VARIABLE_INVALID, e);
        }
    }

    private long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return end.toInstant(ZoneOffset.UTC).toEpochMilli() - start.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void recordExecutionLog(Long tenantId,
                                    Long projectId,
                                    Long executionId,
                                    Long scenarioStepId,
                                    String level,
                                    String message,
                                    Map<String, Object> extra) {
        if (projectId == null) {
            return;
        }
        ExecutionLogCreateRequest request = new ExecutionLogCreateRequest();
        request.setExecutionId(executionId);
        request.setScenarioStepId(scenarioStepId);
        request.setLevel(level);
        request.setMessage(truncateMessage(message));
        request.setExtra(extra);
        executionLogService.appendLog(tenantId, projectId, request);
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= 950) {
            return message;
        }
        return message.substring(0, 947) + "...";
    }
}
