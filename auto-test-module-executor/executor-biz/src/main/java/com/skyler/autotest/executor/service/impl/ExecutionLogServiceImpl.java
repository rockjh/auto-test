package com.skyler.autotest.executor.service.impl;

import com.skyler.autotest.executor.api.dto.ExecutionLogResponse;
import com.skyler.autotest.executor.dal.dataobject.ExecutionLogDO;
import com.skyler.autotest.executor.error.ExecutorErrorCode;
import com.skyler.autotest.executor.service.ExecutionLogService;
import com.skyler.autotest.executor.service.dto.ExecutionLogCreateRequest;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.infra.core.page.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 执行日志服务实现，负责分表创建、写入与分页查询。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionLogServiceImpl implements ExecutionLogService {

    private static final String LOG_TABLE_TEMPLATE = "autotest_execution_log_template";
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;

    private final Set<Long> ensuredProjects = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<PendingLog> bufferQueue = new LinkedBlockingQueue<>(5000);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    @PostConstruct
    public void start() {
        running.set(true);
        worker = new Thread(this::flushLoop, "execution-log-flusher");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
        flushRemaining();
    }

    @Override
    public void appendLog(Long tenantId, Long projectId, ExecutionLogCreateRequest request) {
        validateIds(tenantId, projectId, request);
        String tableName = resolveTableName(projectId);
        ensureTableExists(tableName);

        ExecutionLogDO logDO = buildLogDO(tenantId, request);
        PendingLog pending = new PendingLog(tableName, logDO);
        if (!bufferQueue.offer(pending)) {
            log.warn("execution log buffer full, fallback to sync insert");
            insertBatch(tableName, List.of(logDO));
        }
    }

    @Override
    public PageResult<ExecutionLogResponse> pageLogs(Long projectId, Long executionId, int pageNo, int pageSize) {
        if (projectId == null || executionId == null) {
            return PageResult.empty();
        }
        String tableName = resolveTableName(projectId);
        ensureTableExists(tableName);

        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(Math.min(pageSize, 100), 1);
        long offset = (long) (safePageNo - 1) * safePageSize;

        Long total = queryTotal(tableName, executionId);
        if (total == 0) {
            return PageResult.empty();
        }

        String querySql = "SELECT id, tenant_id, execution_id, scenario_step_id, log_time, level, message, extra, notification_channel, notification_status "
                + "FROM " + tableName + " WHERE execution_id = :executionId AND deleted = 0 "
                + "ORDER BY log_time ASC, id ASC LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("executionId", executionId)
                .addValue("limit", safePageSize)
                .addValue("offset", offset);
        List<ExecutionLogDO> list = namedParameterJdbcTemplate.query(querySql, params,
                BeanPropertyRowMapper.newInstance(ExecutionLogDO.class));
        List<ExecutionLogResponse> responses = list.stream().map(this::convert).toList();
        return new PageResult<>(responses, total);
    }

    private void flushLoop() {
        while (running.get() || !bufferQueue.isEmpty()) {
            try {
                PendingLog first = bufferQueue.poll(1, TimeUnit.SECONDS);
                if (first == null) {
                    continue;
                }
                List<PendingLog> batch = new ArrayList<>();
                batch.add(first);
                bufferQueue.drainTo(batch, DEFAULT_BATCH_SIZE - 1);
                Map<String, List<ExecutionLogDO>> grouped = batch.stream()
                        .collect(Collectors.groupingBy(PendingLog::tableName, LinkedHashMap::new,
                                Collectors.mapping(PendingLog::log, Collectors.toList())));
                grouped.forEach((table, logs) -> {
                    try {
                        insertBatch(table, logs);
                    } catch (Exception ex) {
                        log.error("flush execution log failed, table={}", table, ex);
                    }
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void flushRemaining() {
        List<PendingLog> remaining = new ArrayList<>();
        bufferQueue.drainTo(remaining);
        if (remaining.isEmpty()) {
            return;
        }
        Map<String, List<ExecutionLogDO>> grouped = remaining.stream()
                .collect(Collectors.groupingBy(PendingLog::tableName, Collectors.mapping(PendingLog::log, Collectors.toList())));
        grouped.forEach((table, logs) -> {
            try {
                insertBatch(table, logs);
            } catch (Exception ex) {
                log.error("flush remaining execution log failed, table={}", table, ex);
            }
        });
    }

    private void insertBatch(String tableName, List<ExecutionLogDO> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO " + tableName + " (id, tenant_id, execution_id, scenario_step_id, log_time, level, message, extra, notification_channel, notification_status, creator, create_time, updater, update_time, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try {
            List<Object[]> args = logs.stream().map(this::buildArgs).toList();
            jdbcTemplate.batchUpdate(sql, args);
        } catch (DataAccessException ex) {
            log.error("batch insert execution log failed, table={}", tableName, ex);
            throw new BizException(ExecutorErrorCode.EXECUTION_LOG_APPEND_FAILED, ex);
        }
    }

    private ExecutionLogDO buildLogDO(Long tenantId, ExecutionLogCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ExecutionLogDO logDO = new ExecutionLogDO();
        logDO.setId(bizIdGenerator.nextId());
        logDO.setTenantId(tenantId);
        logDO.setExecutionId(request.getExecutionId());
        logDO.setScenarioStepId(request.getScenarioStepId());
        logDO.setLogTime(now);
        logDO.setLevel(request.getLevel());
        logDO.setMessage(request.getMessage());
        logDO.setExtra(serializeExtra(request.getExtra()));
        logDO.setNotificationChannel(request.getNotificationChannel());
        logDO.setNotificationStatus(request.getNotificationStatus());
        logDO.setCreator("system");
        logDO.setCreateTime(now);
        logDO.setUpdater("system");
        logDO.setUpdateTime(now);
        logDO.setDeleted(Boolean.FALSE);
        return logDO;
    }

    private Object[] buildArgs(ExecutionLogDO logDO) {
        return new Object[]{
                logDO.getId(),
                logDO.getTenantId(),
                logDO.getExecutionId(),
                logDO.getScenarioStepId(),
                logDO.getLogTime(),
                logDO.getLevel(),
                logDO.getMessage(),
                logDO.getExtra(),
                logDO.getNotificationChannel(),
                logDO.getNotificationStatus(),
                logDO.getCreator(),
                logDO.getCreateTime(),
                logDO.getUpdater(),
                logDO.getUpdateTime()
        };
    }

    private void validateIds(Long tenantId, Long projectId, ExecutionLogCreateRequest request) {
        if (tenantId == null || tenantId <= 0) {
            throw new BizException(ExecutorErrorCode.LOG_TENANT_ID_REQUIRED);
        }
        if (projectId == null || projectId <= 0) {
            throw new BizException(ExecutorErrorCode.LOG_PROJECT_ID_REQUIRED);
        }
        if (request.getExecutionId() == null) {
            throw new BizException(ExecutorErrorCode.LOG_EXECUTION_ID_REQUIRED);
        }
        if (request.getLevel() == null) {
            request.setLevel("INFO");
        }
        if (request.getMessage() == null) {
            request.setMessage("");
        }
    }

    private String resolveTableName(Long projectId) {
        return "autotest_execution_log_" + projectId;
    }

    private void ensureTableExists(String tableName) {
        Long projectId = extractProjectId(tableName);
        if (projectId != null && ensuredProjects.contains(projectId)) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " LIKE " + LOG_TABLE_TEMPLATE);
            if (projectId != null) {
                ensuredProjects.add(projectId);
            }
        } catch (DataAccessException ex) {
            log.error("ensure execution log table failed, table={}", tableName, ex);
            throw new BizException(ExecutorErrorCode.EXECUTION_LOG_TABLE_FAILED, ex);
        }
    }

    private Long extractProjectId(String tableName) {
        if (tableName == null) {
            return null;
        }
        String prefix = "autotest_execution_log_";
        if (!tableName.startsWith(prefix)) {
            return null;
        }
        try {
            return Long.parseLong(tableName.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long queryTotal(String tableName, Long executionId) {
        String countSql = "SELECT COUNT(1) AS total FROM " + tableName + " WHERE execution_id = ? AND deleted = 0";
        try {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(countSql, executionId);
            if (rowSet.next()) {
                return rowSet.getLong("total");
            }
            return 0L;
        } catch (DataAccessException ex) {
            log.error("count execution logs failed, executionId={}", executionId, ex);
            throw new BizException(ExecutorErrorCode.EXECUTION_LOG_QUERY_FAILED, ex);
        }
    }

    private String serializeExtra(Object extra) {
        if (extra == null) {
            return null;
        }
        if (extra instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (JsonProcessingException e) {
            log.warn("serialize extra failed, fallback to toString", e);
            return extra.toString();
        }
    }

    private ExecutionLogResponse convert(ExecutionLogDO logDO) {
        ExecutionLogResponse response = new ExecutionLogResponse();
        response.setId(logDO.getId());
        response.setExecutionId(logDO.getExecutionId());
        response.setScenarioStepId(logDO.getScenarioStepId());
        response.setLogTime(logDO.getLogTime());
        response.setLevel(logDO.getLevel());
        response.setMessage(logDO.getMessage());
        response.setExtra(logDO.getExtra());
        response.setNotificationChannel(logDO.getNotificationChannel());
        response.setNotificationStatus(logDO.getNotificationStatus());
        return response;
    }

    private record PendingLog(String tableName, ExecutionLogDO log) {
    }
}
