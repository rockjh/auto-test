package com.skyler.autotest.executor.service;

import com.skyler.autotest.executor.api.dto.ExecutionLogResponse;
import com.skyler.autotest.executor.service.dto.ExecutionLogCreateRequest;
import com.skyler.autotest.infra.core.page.PageResult;

/**
 * 执行日志服务，负责日志分表写入与查询。
 */
public interface ExecutionLogService {

    /**
     * 追加执行日志，如目标分表不存在会自动按模板创建。
     *
     * @param tenantId 租户编号
     * @param projectId 项目编号，用于确定日志分表
     * @param request 日志写入参数
     */
    void appendLog(Long tenantId, Long projectId, ExecutionLogCreateRequest request);

    /**
     * 按执行记录分页查询日志。
     *
     * @param projectId 项目编号
     * @param executionId 执行记录编号
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页容量
     * @return 日志分页结果
     */
    PageResult<ExecutionLogResponse> pageLogs(Long projectId, Long executionId, int pageNo, int pageSize);
}
