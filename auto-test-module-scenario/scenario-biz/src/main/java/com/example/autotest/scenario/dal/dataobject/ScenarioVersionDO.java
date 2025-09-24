package com.example.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_scenario_version")
public class ScenarioVersionDO extends BaseDO {
    private Long scenarioId;
    private String versionNo;
    private String content;
    private String comment;
    private Long publisherId;
    private String publisherName;
}
