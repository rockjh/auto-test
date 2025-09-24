package com.example.autotest.executor.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("autotest_execution")
public class ExecutionDO extends BaseDO {
    private Long scenarioId;
    private Long projectId;
    private Long envId;
    private String triggerType;
    private String triggerUser;
    private String externalJobId;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String summary;
    private String notifyChannels;
    private String remark;
}
