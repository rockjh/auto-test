package com.skyler.autotest.infra.core.page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果统一封装，供所有列表接口返回使用，兼容 Swagger 自动文档。
 *
 * @param <T> 列表元素类型
 */
@Data
@AllArgsConstructor
@Schema(description = "分页结果包装")
public class PageResult<T> {

    @Schema(description = "当前页数据列表")
    private List<T> list;

    @Schema(description = "符合条件的总记录数", example = "120")
    private long total;

    /**
     * 返回一个空的分页结果，用于查询结果为空或初始化场景。
     *
     * @param <T> 列表元素类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L);
    }
}
