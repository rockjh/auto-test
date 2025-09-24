package com.example.autotest.scenario.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.autotest.infra.core.exception.BizException;
import com.example.autotest.infra.core.id.BizIdGenerator;
import com.example.autotest.scenario.api.ScenarioService;
import com.example.autotest.scenario.api.dto.ScenarioCreateRequest;
import com.example.autotest.scenario.api.dto.ScenarioPublishRequest;
import com.example.autotest.scenario.api.dto.ScenarioResponse;
import com.example.autotest.scenario.converter.ScenarioConverter;
import com.example.autotest.scenario.dal.dataobject.ScenarioDO;
import com.example.autotest.scenario.dal.dataobject.ScenarioStepDO;
import com.example.autotest.scenario.dal.dataobject.ScenarioVersionDO;
import com.example.autotest.scenario.dal.mapper.ScenarioMapper;
import com.example.autotest.scenario.dal.mapper.ScenarioStepMapper;
import com.example.autotest.scenario.dal.mapper.ScenarioVersionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final ScenarioVersionMapper scenarioVersionMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScenario(Long tenantId, ScenarioCreateRequest request) {
        ScenarioDO scenario = new ScenarioDO();
        scenario.setId(bizIdGenerator.nextId());
        scenario.setTenantId(tenantId);
        scenario.setProjectId(request.getProjectId());
        scenario.setName(request.getName());
        scenario.setDefaultEnvId(request.getDefaultEnvId());
        scenario.setStatus(0);
        scenario.setNeedReview(Boolean.FALSE);
        scenario.setRemark(request.getRemark());
        scenarioMapper.insert(scenario);
        saveSteps(scenario, request.getSteps());
        return scenario.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScenario(Long scenarioId, ScenarioCreateRequest request) {
        ScenarioDO scenario = scenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new BizException(404, "scenario not found");
        }
        scenario.setName(request.getName());
        scenario.setDefaultEnvId(request.getDefaultEnvId());
        scenario.setRemark(request.getRemark());
        scenario.setNeedReview(Boolean.TRUE);
        scenarioMapper.updateById(scenario);
        scenarioStepMapper.delete(new LambdaQueryWrapper<ScenarioStepDO>().eq(ScenarioStepDO::getScenarioId, scenarioId));
        saveSteps(scenario, request.getSteps());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishScenario(Long scenarioId, ScenarioPublishRequest request) {
        ScenarioDO scenario = scenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new BizException(404, "scenario not found");
        }
        List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getScenarioId, scenarioId)
                .orderByAsc(ScenarioStepDO::getOrderNo));
        scenario.setStatus(1);
        scenario.setNeedReview(Boolean.FALSE);
        scenarioMapper.updateById(scenario);

        ScenarioVersionDO version = new ScenarioVersionDO();
        version.setId(bizIdGenerator.nextId());
        version.setTenantId(scenario.getTenantId());
        version.setScenarioId(scenario.getId());
        version.setVersionNo("v" + LocalDateTime.now());
        version.setComment(request != null ? request.getComment() : null);
        try {
            version.setContent(objectMapper.writeValueAsString(ScenarioConverter.toResponse(scenario, steps)));
        } catch (JsonProcessingException e) {
            throw new BizException(500, "failed to serialize scenario");
        }
        scenarioVersionMapper.insert(version);
    }

    @Override
    public ScenarioResponse getScenario(Long scenarioId) {
        ScenarioDO scenario = scenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            return null;
        }
        List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getScenarioId, scenarioId)
                .orderByAsc(ScenarioStepDO::getOrderNo));
        return ScenarioConverter.toResponse(scenario, steps);
    }

    @Override
    public List<ScenarioResponse> listByProject(Long projectId) {
        List<ScenarioDO> scenarios = scenarioMapper.selectList(new LambdaQueryWrapper<ScenarioDO>()
                .eq(ScenarioDO::getProjectId, projectId)
                .eq(ScenarioDO::getDeleted, false));
        return scenarios.stream()
                .map(scenario -> {
                    List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                            .eq(ScenarioStepDO::getScenarioId, scenario.getId())
                            .orderByAsc(ScenarioStepDO::getOrderNo));
                    return ScenarioConverter.toResponse(scenario, steps);
                })
                .collect(Collectors.toList());
    }

    private void saveSteps(ScenarioDO scenario, List<ScenarioCreateRequest.ScenarioStepRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            throw new BizException(400, "scenario steps required");
        }
        int index = 1;
        List<ScenarioCreateRequest.ScenarioStepRequest> sorted = requests.stream()
                .sorted(Comparator.comparing(step -> step.getOrderNo() != null ? step.getOrderNo() : Integer.MAX_VALUE))
                .toList();
        for (ScenarioCreateRequest.ScenarioStepRequest stepRequest : sorted) {
            ScenarioStepDO step = new ScenarioStepDO();
            step.setId(bizIdGenerator.nextId());
            step.setTenantId(scenario.getTenantId());
            step.setScenarioId(scenario.getId());
            step.setCurlVariantId(stepRequest.getCurlVariantId());
            step.setStepAlias(stepRequest.getStepAlias());
            step.setOrderNo(stepRequest.getOrderNo() != null ? stepRequest.getOrderNo() : index++);
            step.setVariableMapping(stepRequest.getVariableMapping());
            step.setInvokeOptions(stepRequest.getInvokeOptions());
            scenarioStepMapper.insert(step);
        }
    }
}
