package com.skyler.autotest.executor.converter;

import com.skyler.autotest.executor.api.dto.ExecutionDetailResponse;
import com.skyler.autotest.executor.api.dto.ExecutionResponse;
import com.skyler.autotest.executor.dal.dataobject.ExecutionDO;
import com.skyler.autotest.executor.dal.dataobject.ExecutionDetailDO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行领域对象与 API DTO 的转换器。
 */
public class ExecutionConverter {

    private ExecutionConverter() {
    }

    public static ExecutionResponse toResponse(ExecutionDO execution, List<ExecutionDetailDO> details) {
        if (execution == null) {
            return null;
        }
        ExecutionResponse response = new ExecutionResponse();
        response.setId(execution.getId());
        response.setScenarioId(execution.getScenarioId());
        response.setStatus(execution.getStatus());
        response.setStartTime(execution.getStartTime());
        response.setEndTime(execution.getEndTime());
        response.setDurationMs(execution.getDurationMs());
        response.setRemark(execution.getRemark());
        if (details != null) {
            List<ExecutionDetailResponse> detailResponses = details.stream().map(detail -> {
                ExecutionDetailResponse dto = new ExecutionDetailResponse();
                dto.setId(detail.getId());
                dto.setScenarioStepId(detail.getScenarioStepId());
                dto.setStepOrder(detail.getStepOrder());
                dto.setStatus(detail.getStatus());
                dto.setRequestSnapshot(detail.getRequestSnapshot());
                dto.setResponseSnapshot(detail.getResponseSnapshot());
                dto.setErrorMessage(detail.getErrorMessage());
                dto.setStartTime(detail.getStartTime());
                dto.setEndTime(detail.getEndTime());
                return dto;
            }).collect(Collectors.toList());
            response.setSteps(detailResponses);
        }
        return response;
    }
}
