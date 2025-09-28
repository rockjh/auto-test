package com.skyler.autotest.executor.dal.mapper;

import com.skyler.autotest.executor.dal.dataobject.ExecutionDetailDO;
import com.skyler.autotest.infra.core.mybatis.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExecutionDetailMapper extends BaseMapperX<ExecutionDetailDO> {
}
