package com.skyler.autotest.executor.api;

import com.skyler.autotest.executor.api.dto.ExecutionLogResponse;
import com.skyler.autotest.executor.api.dto.ExecutionResponse;
import com.skyler.autotest.executor.api.dto.ExecutionTriggerRequest;
import com.skyler.autotest.infra.core.page.PageResult;

public interface ExecutionService {

    /**
     * 触发场景执行。
     *
     * @param tenantId 租户编号
     * @param request 执行触发参数
     * @return 执行记录编号
     */
    Long trigger(Long tenantId, ExecutionTriggerRequest request);

    /**
     * 查询执行详情。
     *
     * @param executionId 执行记录编号
     * @return 执行详情
     */
    ExecutionResponse getDetail(Long executionId);

    /**
     * 按执行记录分页查询日志。
     *
     * @param executionId 执行记录编号
     * @param pageNo 页码
     * @param pageSize 分页大小
     * @return 日志分页结果
     */
    PageResult<ExecutionLogResponse> pageLogs(Long executionId, int pageNo, int pageSize);
}
