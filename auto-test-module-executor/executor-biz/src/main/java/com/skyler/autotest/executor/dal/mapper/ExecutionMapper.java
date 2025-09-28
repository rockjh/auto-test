package com.skyler.autotest.executor.dal.mapper;

import com.skyler.autotest.executor.dal.dataobject.ExecutionDO;
import com.skyler.autotest.infra.core.mybatis.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExecutionMapper extends BaseMapperX<ExecutionDO> {
}
