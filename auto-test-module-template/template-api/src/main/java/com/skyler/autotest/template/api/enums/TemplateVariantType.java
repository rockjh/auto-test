package com.skyler.autotest.template.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * `curl` 模板类型枚举，便于在多模板场景下统一管理。
 */
@Getter
@Schema(description = "模板类型枚举")
public enum TemplateVariantType {

    @Schema(description = "最少参数模板，仅包含必需字段")
    MINIMAL("minimal"),

    @Schema(description = "全量参数模板，尽量覆盖所有可配置字段")
    FULL("full");

    private final String type;

    TemplateVariantType(String type) {
        this.type = type;
    }

    public static TemplateVariantType fromType(String type) {
        return findByType(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown template variant type: " + type));
    }

    /**
     * 尝试根据类型编码映射枚举值，忽略大小写。
     *
     * @param type 类型编码
     * @return 匹配的枚举（若无则为空）
     */
    public static Optional<TemplateVariantType> findByType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.type.equalsIgnoreCase(type))
                .findFirst();
    }
}
