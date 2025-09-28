package com.skyler.autotest.template.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 模板变更类型枚举，统一描述模板生成或同步时的触发来源。
 */
@Getter
@Schema(description = "模板变更类型枚举")
public enum TemplateChangeType {

    @Schema(description = "手动触发生成或重新生成")
    MANUAL("MANUAL", "手动触发"),

    @Schema(description = "Swagger 新增接口分组时自动生成")
    CREATE("CREATE", "新增分组同步"),

    @Schema(description = "Swagger 更新接口分组时重新生成")
    SYNC_UPDATE("SYNC_UPDATE", "同步更新"),

    @Schema(description = "接口分组被删除时标记模板失效")
    DELETE("DELETE", "分组删除");

    private final String code;
    private final String description;

    TemplateChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 返回对外事件中使用的变更类型编码。
     *
     * @return 变更类型编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回枚举的业务描述，便于日志与调试。
     *
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }
}
