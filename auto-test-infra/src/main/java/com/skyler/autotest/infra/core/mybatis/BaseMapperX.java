package com.skyler.autotest.infra.core.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 扩展 BaseMapper 的统一入口，后续可追加通用查询能力。
 *
 * @param <T> 实体类型
 */
public interface BaseMapperX<T> extends BaseMapper<T> {
}
