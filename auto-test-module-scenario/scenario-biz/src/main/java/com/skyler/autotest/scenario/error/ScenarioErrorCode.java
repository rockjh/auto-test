package com.skyler.autotest.scenario.error;

import com.skyler.autotest.infra.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 场景管理域错误码枚举，统一维护业务异常码与默认提示。
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioErrorCode implements ErrorCode {

    /** 场景不存在。 */
    SCENARIO_NOT_FOUND(42001, "场景不存在"),

    /** 场景所属项目不可变更。 */
    SCENARIO_PROJECT_IMMUTABLE(42002, "场景所属项目不可变更"),

    /** 场景步骤必须至少配置一个。 */
    SCENARIO_STEPS_REQUIRED(42003, "场景步骤不能为空"),

    /** 场景步骤执行顺序重复。 */
    SCENARIO_STEP_ORDER_DUPLICATE(42004, "场景步骤执行顺序重复"),

    /** 引用的 curl 模板不存在。 */
    CURL_VARIANT_NOT_FOUND(42005, "引用的 curl 模板不存在"),

    /** 模板不属于当前项目。 */
    CURL_VARIANT_PROJECT_MISMATCH(42006, "模板不属于当前项目"),

    /** 场景内容序列化失败。 */
    SCENARIO_SERIALIZE_FAILED(42007, "场景内容序列化失败"),

    /** 变量映射序列化失败。 */
    VARIABLE_SERIALIZE_FAILED(42008, "变量映射序列化失败");

    private final int code;
    private final String message;
}
