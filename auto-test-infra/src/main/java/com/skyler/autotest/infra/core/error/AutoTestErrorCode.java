package com.skyler.autotest.infra.core.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系统通用错误码枚举，覆盖平台级别的异常场景。
 */
@Getter
@RequiredArgsConstructor
public enum AutoTestErrorCode implements ErrorCode {

    /** 成功。 */
    SUCCESS(0, "OK"),

    /** 请求参数不合法。 */
    BAD_REQUEST(400, "请求参数不合法"),

    /** 未登录或登录已失效。 */
    UNAUTHORIZED(401, "未登录或登录已失效"),

    /** 资源不存在。 */
    NOT_FOUND(404, "资源不存在"),

    /** 内部错误。 */
    INTERNAL_ERROR(500, "系统内部异常"),

    /** JSON 序列化失败。 */
    JSON_SERIALIZE_FAILED(50001, "JSON 序列化失败"),

    /** 模板类型不支持。 */
    TEMPLATE_VARIANT_TYPE_UNSUPPORTED(40010, "模板类型不支持"),

    /** 模板关联接口分组不存在。 */
    TEMPLATE_GROUP_NOT_FOUND(40402, "模板关联接口分组不存在"),

    /** 模板不存在。 */
    TEMPLATE_VARIANT_NOT_FOUND(40403, "模板不存在"),

    /** 模板生成器未注册。 */
    TEMPLATE_GENERATOR_NOT_REGISTERED(50003, "模板生成器未注册"),

    /** Swagger 请求定义解析失败。 */
    TEMPLATE_OPERATION_PARSE_FAILED(50004, "Swagger 请求定义解析失败"),

    /** 模板环境 Header 解析失败。 */
    TEMPLATE_ENV_HEADER_PARSE_FAILED(50005, "模板环境 Header 解析失败"),

    /** 模板环境变量解析失败。 */
    TEMPLATE_ENV_VARIABLE_PARSE_FAILED(50006, "模板环境变量解析失败"),

    /** 模板规则序列化失败。 */
    TEMPLATE_RULE_SERIALIZE_FAILED(50007, "模板规则序列化失败"),

    /** 模板参数规则内容非法。 */
    TEMPLATE_PARAM_RULE_INVALID(50008, "模板参数规则内容非法"),

    /** 差异快照不存在。 */
    DIFF_NOT_FOUND(40401, "差异快照不存在"),

    /** 差异复核请求不能为空。 */
    DIFF_REVIEW_REQUEST_REQUIRED(40001, "差异复核请求不能为空"),

    /** 差异复核状态不合法。 */
    DIFF_REVIEW_STATUS_INVALID(40002, "差异复核状态仅支持通过或驳回"),

    /** 差异快照载荷序列化失败。 */
    DIFF_PAYLOAD_SERIALIZE_FAILED(50002, "差异快照载荷序列化失败");

    private final int code;
    private final String message;
}
