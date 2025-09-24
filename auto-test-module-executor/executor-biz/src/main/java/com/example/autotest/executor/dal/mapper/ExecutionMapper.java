package com.example.autotest.executor.dal.mapper;

import com.example.autotest.executor.dal.dataobject.ExecutionDO;
import com.example.autotest.infra.core.mybatis.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExecutionMapper extends BaseMapperX<ExecutionDO> {
}
