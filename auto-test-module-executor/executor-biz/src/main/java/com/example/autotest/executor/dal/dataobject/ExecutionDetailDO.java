package com.example.autotest.executor.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("autotest_execution_detail")
public class ExecutionDetailDO extends BaseDO {
    private Long executionId;
    private Long scenarioStepId;
    private Integer stepOrder;
    private Integer status;
    private Integer retryCount;
    private String requestSnapshot;
    private String responseSnapshot;
    private String variablesSnapshot;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;
}
