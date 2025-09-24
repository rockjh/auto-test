package com.example.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_scenario_step")
public class ScenarioStepDO extends BaseDO {
    private Long scenarioId;
    private Integer orderNo;
    private String stepAlias;
    private Long curlVariantId;
    private String invokeOptions;
    private String variableMapping;
    private String extractors;
    private String conditionConfig;
    private String remark;
}
