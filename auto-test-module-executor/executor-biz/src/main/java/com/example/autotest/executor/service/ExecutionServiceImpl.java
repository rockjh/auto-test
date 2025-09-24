package com.example.autotest.executor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.autotest.executor.api.ExecutionService;
import com.example.autotest.executor.api.dto.ExecutionResponse;
import com.example.autotest.executor.api.dto.ExecutionTriggerRequest;
import com.example.autotest.executor.converter.ExecutionConverter;
import com.example.autotest.executor.dal.dataobject.ExecutionDO;
import com.example.autotest.executor.dal.dataobject.ExecutionDetailDO;
import com.example.autotest.executor.dal.mapper.ExecutionDetailMapper;
import com.example.autotest.executor.dal.mapper.ExecutionMapper;
import com.example.autotest.infra.core.exception.BizException;
import com.example.autotest.infra.core.id.BizIdGenerator;
import com.example.autotest.infra.event.ScenarioExecutionEvent;
import com.example.autotest.template.api.TemplateService;
import com.example.autotest.template.api.dto.TemplatePreviewRequest;
import com.example.autotest.scenario.dal.dataobject.ScenarioDO;
import com.example.autotest.scenario.dal.dataobject.ScenarioStepDO;
import com.example.autotest.scenario.dal.mapper.ScenarioMapper;
import com.example.autotest.scenario.dal.mapper.ScenarioStepMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ExecutionMapper executionMapper;
    private final ExecutionDetailMapper executionDetailMapper;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final TemplateService templateService;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 触发场景执行，当前实现串行调用模板预览模拟执行结果，并发布执行事件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long trigger(Long tenantId, ExecutionTriggerRequest request) {
        ScenarioDO scenario = scenarioMapper.selectById(request.getScenarioId());
        if (scenario == null) {
            throw new BizException(404, "scenario not found");
        }
        List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getScenarioId, scenario.getId())
                .orderByAsc(ScenarioStepDO::getOrderNo));
        if (steps.isEmpty()) {
            throw new BizException(400, "scenario steps empty");
        }

        ExecutionDO execution = new ExecutionDO();
        execution.setId(bizIdGenerator.nextId());
        execution.setTenantId(tenantId);
        execution.setScenarioId(scenario.getId());
        execution.setProjectId(scenario.getProjectId());
        execution.setEnvId(request.getEnvId() != null ? request.getEnvId() : scenario.getDefaultEnvId());
        execution.setTriggerType("manual");
        execution.setTriggerUser("system");
        execution.setStatus(1);
        execution.setStartTime(LocalDateTime.now());
        execution.setRemark(request.getRemark());
        executionMapper.insert(execution);

        Map<String, Object> variables = new HashMap<>();
        variables.put("host", "http://localhost");

        try {
            int stepOrder = 1;
            for (ScenarioStepDO step : steps) {
                ExecutionDetailDO detail = new ExecutionDetailDO();
                detail.setId(bizIdGenerator.nextId());
                detail.setTenantId(tenantId);
                detail.setExecutionId(execution.getId());
                detail.setScenarioStepId(step.getId());
                detail.setStepOrder(stepOrder++);
                detail.setStatus(1);
                detail.setStartTime(LocalDateTime.now());
                TemplatePreviewRequest previewRequest = new TemplatePreviewRequest();
                previewRequest.setVariables(new HashMap<>(variables));
                String command = templateService.preview(step.getCurlVariantId(), previewRequest);
                detail.setRequestSnapshot(command);

                String responsePayload = buildMockResponse(step, command, variables);
                detail.setResponseSnapshot(responsePayload);
                detail.setVariablesSnapshot(writeJson(variables));
                detail.setStatus(2);
                detail.setEndTime(LocalDateTime.now());
                executionDetailMapper.insert(detail);
            }
            execution.setStatus(2);
            execution.setEndTime(LocalDateTime.now());
            execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));
            executionMapper.updateById(execution);
        } catch (Exception ex) {
            execution.setStatus(3);
            execution.setEndTime(LocalDateTime.now());
            execution.setDurationMs(calculateDuration(execution.getStartTime(), execution.getEndTime()));
            execution.setSummary(ex.getMessage());
            executionMapper.updateById(execution);
            log.error("execution failed", ex);
            throw ex;
        } finally {
            eventPublisher.publishEvent(new ScenarioExecutionEvent(
                    tenantId,
                    execution.getId(),
                    execution.getScenarioId(),
                    execution.getStatus(),
                    execution.getDurationMs()
            ));
        }
        return execution.getId();
    }

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

    private String buildMockResponse(ScenarioStepDO step, String command, Map<String, Object> variables) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "success");
        payload.put("step", step.getId());
        payload.put("command", command);
        payload.put("variables", variables);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "failed to serialize response");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "failed to serialize variables");
        }
    }

    private long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return end.toInstant(ZoneOffset.UTC).toEpochMilli() - start.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
