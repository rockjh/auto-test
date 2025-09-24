package com.example.autotest.executor.converter;

import com.example.autotest.executor.api.dto.ExecutionDetailResponse;
import com.example.autotest.executor.api.dto.ExecutionResponse;
import com.example.autotest.executor.dal.dataobject.ExecutionDO;
import com.example.autotest.executor.dal.dataobject.ExecutionDetailDO;

import java.util.List;
import java.util.stream.Collectors;

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
