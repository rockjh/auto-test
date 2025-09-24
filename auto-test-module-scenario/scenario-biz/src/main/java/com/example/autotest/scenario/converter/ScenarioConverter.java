package com.example.autotest.scenario.converter;

import com.example.autotest.scenario.api.dto.ScenarioResponse;
import com.example.autotest.scenario.dal.dataobject.ScenarioDO;
import com.example.autotest.scenario.dal.dataobject.ScenarioStepDO;

import java.util.List;
import java.util.stream.Collectors;

public class ScenarioConverter {

    private ScenarioConverter() {
    }

    public static ScenarioResponse toResponse(ScenarioDO scenario, List<ScenarioStepDO> steps) {
        if (scenario == null) {
            return null;
        }
        ScenarioResponse response = new ScenarioResponse();
        response.setId(scenario.getId());
        response.setProjectId(scenario.getProjectId());
        response.setName(scenario.getName());
        response.setStatus(scenario.getStatus());
        response.setDefaultEnvId(scenario.getDefaultEnvId());
        response.setRemark(scenario.getRemark());
        response.setNeedReview(scenario.getNeedReview());
        response.setCreateTime(scenario.getCreateTime());
        if (steps != null) {
            List<ScenarioResponse.ScenarioStepVO> stepVOS = steps.stream().map(step -> {
                ScenarioResponse.ScenarioStepVO vo = new ScenarioResponse.ScenarioStepVO();
                vo.setId(step.getId());
                vo.setOrderNo(step.getOrderNo());
                vo.setStepAlias(step.getStepAlias());
                vo.setCurlVariantId(step.getCurlVariantId());
                vo.setVariableMapping(step.getVariableMapping());
                vo.setInvokeOptions(step.getInvokeOptions());
                return vo;
            }).collect(Collectors.toList());
            response.setSteps(stepVOS);
        }
        return response;
    }
}
