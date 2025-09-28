package com.skyler.autotest.swagger.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Swagger 项目来源类型，对应 {@code autotest_project.swagger_type} 字段。
 */
@Getter
@RequiredArgsConstructor
public enum SwaggerProjectSourceTypeEnum {

    /** 远程 URL。 */
    REMOTE_URL(1, "远程 URL"),

    /** 直接存储的 JSON/YAML 文本。 */
    INLINE_TEXT(2, "文本内容"),

    /** 本地或共享存储的文件路径。 */
    FILE_PATH(3, "文件路径");

    private final int code;
    private final String description;

    /**
     * 根据编码解析来源类型，无法匹配时回退到远程 URL。
     *
     * @param code 枚举编码
     * @return 来源类型
     */
    public static SwaggerProjectSourceTypeEnum fromCode(Integer code) {
        if (code == null) {
            return REMOTE_URL;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(REMOTE_URL);
    }
}

