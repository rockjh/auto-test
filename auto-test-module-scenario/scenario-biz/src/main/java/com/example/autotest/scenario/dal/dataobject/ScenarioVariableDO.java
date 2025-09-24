package com.example.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_scenario_variable")
public class ScenarioVariableDO extends BaseDO {
    private Long scenarioId;
    private String scope;
    private Long ownerStepId;
    private String varName;
    private String sourceType;
    private String initValue;
    private String bindingConfig;
    private String remark;
}
