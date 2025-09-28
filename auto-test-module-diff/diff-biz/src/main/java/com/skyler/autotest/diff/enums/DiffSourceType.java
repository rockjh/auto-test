package com.skyler.autotest.diff.enums;

/**
 * 差异来源类型枚举。
 */
public enum DiffSourceType {

    /** Swagger 同步产生的差异。 */
    SWAGGER("swagger"),

    /** 模板调整产生的差异。 */
    TEMPLATE("template"),

    /** 场景调整产生的差异。 */
    SCENARIO("scenario");

    private final String type;

    DiffSourceType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
