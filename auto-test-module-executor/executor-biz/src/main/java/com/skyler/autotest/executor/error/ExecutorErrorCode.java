package com.skyler.autotest.executor.error;

import com.skyler.autotest.infra.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 执行器领域错误码枚举，统一维护执行流程与日志相关的异常码。
 */
@Getter
@RequiredArgsConstructor
public enum ExecutorErrorCode implements ErrorCode {

    /** 场景步骤缺失。 */
    SCENARIO_STEPS_EMPTY(44001, "场景未配置任何步骤"),

    /** 场景正在执行中，无法重复触发。 */
    SCENARIO_LOCKED(44002, "场景正在执行，请稍后重试"),

    /** 加锁被中断。 */
    SCENARIO_LOCK_INTERRUPTED(44003, "获取场景执行锁被中断"),

    /** 触发请求未选择执行环境且场景没有默认环境。 */
    ENVIRONMENT_REQUIRED(44004, "执行环境必填"),

    /** 执行失败的通用兜底错误。 */
    EXECUTION_FAILED(44005, "场景执行失败"),

    /** 执行日志追加失败。 */
    EXECUTION_LOG_APPEND_FAILED(44006, "写入执行日志失败"),

    /** 执行日志表创建失败。 */
    EXECUTION_LOG_TABLE_FAILED(44007, "创建执行日志分表失败"),

    /** 执行日志查询失败。 */
    EXECUTION_LOG_QUERY_FAILED(44008, "查询执行日志失败"),

    /** 租户编号缺失。 */
    LOG_TENANT_ID_REQUIRED(44009, "缺少租户编号"),

    /** 项目编号缺失。 */
    LOG_PROJECT_ID_REQUIRED(44010, "缺少项目编号"),

    /** 执行记录编号缺失。 */
    LOG_EXECUTION_ID_REQUIRED(44011, "缺少执行记录编号"),

    /** curl 命令为空。 */
    CURL_COMMAND_EMPTY(44012, "curl 模板解析结果为空"),

    /** curl 命令格式不正确。 */
    CURL_COMMAND_INVALID(44013, "curl 模板格式不正确"),

    /** curl 命令缺少 URL。 */
    CURL_URL_MISSING(44014, "curl 模板缺少 URL"),

    /** 模板预览返回异常。 */
    TEMPLATE_PREVIEW_FAILED(44015, "模板预览失败");

    private final int code;
    private final String message;
}
