package com.example.autotest.scenario.dal.mapper;

import com.example.autotest.infra.core.mybatis.BaseMapperX;
import com.example.autotest.scenario.dal.dataobject.ScenarioStepDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScenarioStepMapper extends BaseMapperX<ScenarioStepDO> {
}
