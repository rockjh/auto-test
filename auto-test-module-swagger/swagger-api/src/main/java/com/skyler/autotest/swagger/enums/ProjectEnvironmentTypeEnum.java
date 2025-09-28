package com.skyler.autotest.swagger.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 项目环境类型枚举，对应 {@code autotest_project_env.env_type} 字段。
 */
@Getter
@RequiredArgsConstructor
public enum ProjectEnvironmentTypeEnum {

    /** 手工维护型环境。 */
    MANUAL(1, "手工维护"),

    /** 继承其它项目或公共配置。 */
    INHERIT(2, "继承配置");

    private final int code;
    private final String description;

    /**
     * 根据编码解析环境类型，若为空或未匹配则回退到手工维护。
     *
     * @param code 枚举编码
     * @return 环境类型
     */
    public static ProjectEnvironmentTypeEnum resolve(Integer code) {
        if (code == null) {
            return MANUAL;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(MANUAL);
    }
}

