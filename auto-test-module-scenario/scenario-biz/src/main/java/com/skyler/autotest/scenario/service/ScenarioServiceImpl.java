package com.skyler.autotest.scenario.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skyler.autotest.infra.core.error.EnvironmentErrorCode;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.scenario.api.ScenarioService;
import com.skyler.autotest.scenario.api.dto.ScenarioCreateRequest;
import com.skyler.autotest.scenario.api.dto.ScenarioPublishRequest;
import com.skyler.autotest.scenario.api.dto.ScenarioResponse;
import com.skyler.autotest.scenario.api.model.ScenarioVariableMappingRule;
import com.skyler.autotest.scenario.api.util.ScenarioVariableMappingParser;
import com.skyler.autotest.scenario.converter.ScenarioConverter;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioDO;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioStepDO;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioVariableDO;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioVersionDO;
import com.skyler.autotest.scenario.dal.mapper.ScenarioMapper;
import com.skyler.autotest.scenario.dal.mapper.ScenarioStepMapper;
import com.skyler.autotest.scenario.dal.mapper.ScenarioVariableMapper;
import com.skyler.autotest.scenario.dal.mapper.ScenarioVersionMapper;
import com.skyler.autotest.scenario.enums.ScenarioStatus;
import com.skyler.autotest.scenario.error.ScenarioErrorCode;
import com.skyler.autotest.swagger.dal.dataobject.ProjectEnvironmentDO;
import com.skyler.autotest.swagger.dal.mapper.ProjectEnvironmentMapper;
import com.skyler.autotest.template.api.TemplateService;
import com.skyler.autotest.template.api.dto.CurlVariantResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 场景管理核心服务，负责场景的创建、更新、发布及相关校验逻辑。
 */
@Service
@RequiredArgsConstructor
public class ScenarioServiceImpl implements ScenarioService {

    private static final int ENVIRONMENT_STATUS_ENABLED = 1;
    private static final DateTimeFormatter VERSION_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ScenarioMapper scenarioMapper;
    private final ScenarioStepMapper scenarioStepMapper;
    private final ScenarioVariableMapper scenarioVariableMapper;
    private final ScenarioVersionMapper scenarioVersionMapper;
    private final ProjectEnvironmentMapper projectEnvironmentMapper;
    private final TemplateService templateService;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;

    /**
     * 创建场景，包含默认环境校验与步骤落库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScenario(Long tenantId, ScenarioCreateRequest request) {
        validateDefaultEnvironment(tenantId, request.getProjectId(), request.getDefaultEnvId());
        ScenarioDO scenario = new ScenarioDO();
        scenario.setId(bizIdGenerator.nextId());
        scenario.setTenantId(tenantId);
        scenario.setProjectId(request.getProjectId());
        scenario.setName(request.getName());
        scenario.setDefaultEnvId(request.getDefaultEnvId());
        scenario.setStatus(ScenarioStatus.DRAFT.getCode());
        scenario.setNeedReview(Boolean.FALSE);
        scenario.setRemark(request.getRemark());
        scenarioMapper.insert(scenario);
        ScenarioSaveResult saveResult = saveSteps(scenario, request.getSteps());
        if (!Objects.equals(scenario.getNeedReview(), saveResult.isNeedReview())) {
            scenario.setNeedReview(saveResult.isNeedReview());
            scenarioMapper.updateById(scenario);
        }
        return scenario.getId();
    }

    /**
     * 更新场景草稿内容，重新保存步骤并刷新复核标记。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScenario(Long scenarioId, ScenarioCreateRequest request) {
        ScenarioDO scenario = scenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new BizException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
        }
        if (!scenario.getProjectId().equals(request.getProjectId())) {
            throw new BizException(ScenarioErrorCode.SCENARIO_PROJECT_IMMUTABLE);
        }
        validateDefaultEnvironment(scenario.getTenantId(), scenario.getProjectId(), request.getDefaultEnvId());
        scenario.setName(request.getName());
        scenario.setDefaultEnvId(request.getDefaultEnvId());
        scenario.setRemark(request.getRemark());

        ScenarioSaveResult saveResult = saveSteps(scenario, request.getSteps());
        scenario.setNeedReview(saveResult.isNeedReview());
        scenarioMapper.updateById(scenario);
    }

    /**
     * 发布场景，生成版本快照并清除复核标记。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishScenario(Long scenarioId, ScenarioPublishRequest request) {
        ScenarioDO scenario = scenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new BizException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
        }
        List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getScenarioId, scenarioId)
                .orderByAsc(ScenarioStepDO::getOrderNo));
        scenario.setStatus(ScenarioStatus.PUBLISHED.getCode());
        scenario.setNeedReview(Boolean.FALSE);
        scenarioMapper.updateById(scenario);

        ScenarioVersionDO version = new ScenarioVersionDO();
        version.setId(bizIdGenerator.nextId());
        version.setTenantId(scenario.getTenantId());
        version.setScenarioId(scenario.getId());
        version.setVersionNo(generateVersionNo());
        version.setComment(request != null ? request.getComment() : null);
        try {
            version.setContent(objectMapper.writeValueAsString(ScenarioConverter.toResponse(scenario, steps)));
        } catch (JsonProcessingException e) {
            throw new BizException(ScenarioErrorCode.SCENARIO_SERIALIZE_FAILED, e);
        }
        scenarioVersionMapper.insert(version);
    }

    /**
     * 按主键查询场景详情，拼装步骤信息。
     */
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

    /**
     * 查询指定项目下的所有场景及其步骤。
     */
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

    /**
     * 保存场景步骤及变量映射，返回是否需要人工复核。
     */
    private ScenarioSaveResult saveSteps(ScenarioDO scenario, List<ScenarioCreateRequest.ScenarioStepRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            throw new BizException(ScenarioErrorCode.SCENARIO_STEPS_REQUIRED);
        }
        scenarioStepMapper.delete(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getScenarioId, scenario.getId()));
        scenarioVariableMapper.delete(new LambdaQueryWrapper<ScenarioVariableDO>()
                .eq(ScenarioVariableDO::getScenarioId, scenario.getId()));

        List<StepWrapper> wrappers = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            wrappers.add(new StepWrapper(requests.get(i), i));
        }

        wrappers.sort(Comparator
                .comparing((StepWrapper wrapper) -> wrapper.getRequest().getOrderNo(), Comparator.nullsLast(Integer::compareTo))
                .thenComparingInt(StepWrapper::getOriginalIndex));

        Set<Integer> usedOrders = new HashSet<>();
        int nextOrder = 1;
        for (StepWrapper wrapper : wrappers) {
            Integer desired = wrapper.getRequest().getOrderNo();
            if (desired != null) {
                if (!usedOrders.add(desired)) {
                    throw new BizException(ScenarioErrorCode.SCENARIO_STEP_ORDER_DUPLICATE);
                }
                wrapper.setAssignedOrder(desired);
            } else {
                while (usedOrders.contains(nextOrder)) {
                    nextOrder++;
                }
                wrapper.setAssignedOrder(nextOrder);
                usedOrders.add(nextOrder);
                nextOrder++;
            }
        }

        wrappers.sort(Comparator.comparingInt(StepWrapper::getAssignedOrder));

        boolean needReview = false;
        ScenarioStepDO previousStep = null;
        Map<Long, CurlVariantResponse> variantCache = new HashMap<>();
        List<ScenarioVariableDO> variables = new ArrayList<>();

        for (StepWrapper wrapper : wrappers) {
            ScenarioCreateRequest.ScenarioStepRequest stepRequest = wrapper.getRequest();
            CurlVariantResponse variant = resolveVariant(stepRequest.getCurlVariantId(), variantCache);
            if (variant == null) {
                throw new BizException(ScenarioErrorCode.CURL_VARIANT_NOT_FOUND);
            }
            if (!Objects.equals(variant.getProjectId(), scenario.getProjectId())) {
                throw new BizException(ScenarioErrorCode.CURL_VARIANT_PROJECT_MISMATCH);
            }
            if (Boolean.TRUE.equals(variant.getNeedReview())) {
                needReview = true;
            }

            ScenarioVariableMappingParser.ParseResult mappingResult = ScenarioVariableMappingParser.parse(stepRequest.getVariableMapping());
            if (!mappingResult.isSuccess() || mappingResult.hasIncompleteRule()) {
                needReview = true;
            }
            boolean hasMappingConfigured = stepRequest.getVariableMapping() != null && !stepRequest.getVariableMapping().isBlank();
            if ((hasMappingConfigured || (mappingResult.isSuccess() && !mappingResult.getRules().isEmpty())) && previousStep == null) {
                needReview = true;
            }

            ScenarioStepDO step = new ScenarioStepDO();
            step.setId(bizIdGenerator.nextId());
            step.setTenantId(scenario.getTenantId());
            step.setScenarioId(scenario.getId());
            step.setCurlVariantId(stepRequest.getCurlVariantId());
            step.setStepAlias(stepRequest.getStepAlias());
            step.setOrderNo(wrapper.getAssignedOrder());
            step.setInvokeOptions(stepRequest.getInvokeOptions());

            String mappingJson = null;
            if (mappingResult.isSuccess() && !mappingResult.getRules().isEmpty()) {
                mappingJson = mappingResult.getNormalizedJson();
                for (ScenarioVariableMappingRule rule : mappingResult.getRules()) {
                    variables.add(buildScenarioVariable(scenario, step, rule));
                }
            } else if (hasMappingConfigured) {
                mappingJson = stepRequest.getVariableMapping().trim();
            }
            step.setVariableMapping(StringUtils.isNotBlank(mappingJson) ? mappingJson : null);

            scenarioStepMapper.insert(step);
            previousStep = step;
        }

        for (ScenarioVariableDO variable : variables) {
            scenarioVariableMapper.insert(variable);
        }
        return new ScenarioSaveResult(needReview);
    }

    /**
     * 查询模板信息并使用本地缓存降低 RPC 调用次数。
     */
    private CurlVariantResponse resolveVariant(Long variantId,
                                               Map<Long, CurlVariantResponse> cache) {
        if (variantId == null) {
            return null;
        }
        if (cache.containsKey(variantId)) {
            return cache.get(variantId);
        }
        CurlVariantResponse variant = templateService.getVariant(variantId);
        if (variant != null) {
            cache.put(variantId, variant);
        }
        return variant;
    }

    private String generateVersionNo() {
        return "v" + VERSION_FORMATTER.format(LocalDateTime.now());
    }

    /**
     * 根据映射规则生成场景变量记录。
     */
    private ScenarioVariableDO buildScenarioVariable(ScenarioDO scenario,
                                                     ScenarioStepDO step,
                                                     ScenarioVariableMappingRule rule) {
        ScenarioVariableDO variable = new ScenarioVariableDO();
        variable.setId(bizIdGenerator.nextId());
        variable.setTenantId(scenario.getTenantId());
        variable.setScenarioId(scenario.getId());
        variable.setScope("step");
        variable.setOwnerStepId(step.getId());
        variable.setVarName(deriveVarName(rule.getTargetKey()));
        variable.setSourceType("extractor");
        try {
            variable.setBindingConfig(objectMapper.writeValueAsString(rule));
        } catch (JsonProcessingException e) {
            throw new BizException(ScenarioErrorCode.VARIABLE_SERIALIZE_FAILED, e);
        }
        variable.setRemark(rule.getRemark());
        return variable;
    }

    /**
     * 从映射目标键推导变量名称。
     */
    private String deriveVarName(String targetKey) {
        if (targetKey == null) {
            return null;
        }
        int idx = targetKey.lastIndexOf('.');
        if (idx >= 0 && idx < targetKey.length() - 1) {
            return targetKey.substring(idx + 1);
        }
        return targetKey;
    }

    /**
     * 包装步骤请求，记录原始索引与最终分配的顺序。
     */
    private static final class StepWrapper {
        private final ScenarioCreateRequest.ScenarioStepRequest request;
        private final int originalIndex;
        private int assignedOrder;

        private StepWrapper(ScenarioCreateRequest.ScenarioStepRequest request, int originalIndex) {
            this.request = request;
            this.originalIndex = originalIndex;
        }

        public ScenarioCreateRequest.ScenarioStepRequest getRequest() {
            return request;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }

        public int getAssignedOrder() {
            return assignedOrder;
        }

        public void setAssignedOrder(int assignedOrder) {
            this.assignedOrder = assignedOrder;
        }
    }

    /**
     * 保存步骤结果，用于回传复核标记。
     */
    private static final class ScenarioSaveResult {
        private final boolean needReview;

        private ScenarioSaveResult(boolean needReview) {
            this.needReview = needReview;
        }

        public boolean isNeedReview() {
            return needReview;
        }
    }

    /**
     * 校验默认环境是否可用（存在、租户隔离、项目匹配等）。
     */
    private void validateDefaultEnvironment(Long tenantId, Long projectId, Long envId) {
        if (envId == null) {
            return;
        }
        ProjectEnvironmentDO environment = projectEnvironmentMapper.selectById(envId);
        if (environment == null || Boolean.TRUE.equals(environment.getDeleted())) {
            throw new BizException(EnvironmentErrorCode.ENVIRONMENT_NOT_FOUND);
        }
        if (!projectId.equals(environment.getProjectId())) {
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
    }
}
