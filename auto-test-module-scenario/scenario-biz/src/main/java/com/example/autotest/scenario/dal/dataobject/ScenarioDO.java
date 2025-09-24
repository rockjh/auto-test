package com.example.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_scenario")
public class ScenarioDO extends BaseDO {
    private Long projectId;
    private String name;
    private Integer status;
    private String tags;
    private Long ownerId;
    private String ownerName;
    private Long defaultEnvId;
    private String metadata;
    private Boolean needReview;
    private Long lastExecutionId;
    private String remark;
}
