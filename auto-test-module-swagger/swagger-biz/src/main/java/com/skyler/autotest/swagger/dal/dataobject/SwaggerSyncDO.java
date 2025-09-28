package com.skyler.autotest.swagger.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("autotest_swagger_sync")
public class SwaggerSyncDO extends BaseDO {
    private Long projectId;
    private Integer triggerType;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String diffSummary;
    private String errorMessage;
    private Long operatorId;
}
