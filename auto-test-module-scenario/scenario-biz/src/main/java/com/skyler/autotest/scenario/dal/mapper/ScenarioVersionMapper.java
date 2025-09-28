package com.skyler.autotest.scenario.dal.mapper;

import com.skyler.autotest.infra.core.mybatis.BaseMapperX;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioVersionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScenarioVersionMapper extends BaseMapperX<ScenarioVersionDO> {
}
