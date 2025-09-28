package com.skyler.autotest.infra.core.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 执行环境相关错误码枚举，统一约束跨模块对环境的异常描述。
 */
@Getter
@RequiredArgsConstructor
public enum EnvironmentErrorCode implements ErrorCode {

    /** 环境不存在或已被删除。 */
    ENVIRONMENT_NOT_FOUND(43001, "执行环境不存在或已删除"),

    /** 环境与目标项目不匹配。 */
    ENVIRONMENT_PROJECT_MISMATCH(43002, "执行环境不属于指定项目"),

    /** 租户无法访问该环境。 */
    ENVIRONMENT_TENANT_DENIED(43003, "当前租户无权访问该执行环境"),

    /** 环境处于停用状态。 */
    ENVIRONMENT_DISABLED(43004, "执行环境已停用"),

    /** 环境缺少 Host 配置。 */
    ENVIRONMENT_HOST_EMPTY(43005, "执行环境缺少 Host 配置"),

    /** 环境 Header 配置不合法。 */
    ENVIRONMENT_HEADER_INVALID(43006, "执行环境 Header 配置不合法"),

    /** 环境变量配置不合法。 */
    ENVIRONMENT_VARIABLE_INVALID(43007, "执行环境变量配置不合法");

    private final int code;
    private final String message;
}
