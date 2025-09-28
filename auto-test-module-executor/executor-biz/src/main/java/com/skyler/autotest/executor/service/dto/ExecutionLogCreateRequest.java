package com.skyler.autotest.executor.service.dto;

import lombok.Data;

/**
 * 执行日志写入请求。
 */
@Data
public class ExecutionLogCreateRequest {

    private Long executionId;
    private Long scenarioStepId;
    private String level;
    private String message;
    private Object extra;
    private String notificationChannel;
    private Integer notificationStatus;
}
