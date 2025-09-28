package com.skyler.autotest.executor.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行日志数据对象，对应 autotest_execution_log_{projectId} 分表。
 */
@Data
public class ExecutionLogDO {

    private Long id;
    private Long tenantId;
    private Long executionId;
    private Long scenarioStepId;
    private LocalDateTime logTime;
    private String level;
    private String message;
    private String extra;
    private String notificationChannel;
    private Integer notificationStatus;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
