package com.skyler.autotest.swagger.enums;

import com.skyler.autotest.infra.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Swagger 模块错误码枚举，覆盖项目与环境管理的核心异常。
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerErrorCode implements ErrorCode {

    /** Swagger 项目不存在。 */
    PROJECT_NOT_FOUND(40401, "Swagger 项目不存在"),

    /** 项目环境不存在。 */
    ENVIRONMENT_NOT_FOUND(40402, "项目环境不存在"),

    /** swagger 文档解析失败。 */
    SWAGGER_PARSE_FAILED(50001, "Swagger 文档解析失败"),

    /** swagger 来源配置不合法。 */
    INVALID_SWAGGER_SOURCE(40001, "Swagger 来源配置不合法"),

    /** 未授权远程抓取 swagger。 */
    REMOTE_FETCH_NOT_ALLOWED(40002, "未授权远程抓取 Swagger"),

    /** 远程获取 swagger 文档失败。 */
    SWAGGER_REMOTE_FETCH_FAILED(50201, "远程获取 Swagger 文档失败"),

    /** 读取 swagger 内容失败。 */
    SWAGGER_SOURCE_READ_FAILED(50002, "读取 Swagger 内容失败"),

    /** 项目与环境归属不匹配。 */
    ENVIRONMENT_PROJECT_MISMATCH(40003, "环境归属项目不匹配"),

    /** 当前租户无权操作该资源。 */
    TENANT_FORBIDDEN(40301, "当前租户无权操作该资源"),

    /** JSON 解析失败（Headers）。 */
    ENVIRONMENT_HEADER_JSON_INVALID(50003, "环境 Header JSON 解析失败"),

    /** JSON 解析失败（Variables）。 */
    ENVIRONMENT_VARIABLE_JSON_INVALID(50004, "环境变量 JSON 解析失败"),

    /** JSON 序列化失败。 */
    ENVIRONMENT_JSON_SERIALIZE_FAILED(50005, "环境配置 JSON 序列化失败"),

    /** Swagger 操作序列化失败。 */
    SWAGGER_OPERATION_SERIALIZE_FAILED(50006, "Swagger 操作序列化失败"),

    /** Swagger 同步流程执行失败。 */
    SWAGGER_SYNC_FAILED(50007, "Swagger 同步流程执行失败");

    private final int code;
    private final String message;
}
