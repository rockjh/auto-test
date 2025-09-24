package com.example.autotest.executor.api;

import com.example.autotest.executor.api.dto.ExecutionResponse;
import com.example.autotest.executor.api.dto.ExecutionTriggerRequest;

public interface ExecutionService {

    Long trigger(Long tenantId, ExecutionTriggerRequest request);

    ExecutionResponse getDetail(Long executionId);
}
