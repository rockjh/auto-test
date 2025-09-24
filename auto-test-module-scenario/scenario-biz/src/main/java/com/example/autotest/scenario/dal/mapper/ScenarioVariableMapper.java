package com.example.autotest.scenario.dal.mapper;

import com.example.autotest.infra.core.mybatis.BaseMapperX;
import com.example.autotest.scenario.dal.dataobject.ScenarioVariableDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScenarioVariableMapper extends BaseMapperX<ScenarioVariableDO> {
}
