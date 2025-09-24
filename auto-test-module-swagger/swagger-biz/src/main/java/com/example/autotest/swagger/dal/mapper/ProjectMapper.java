package com.example.autotest.swagger.dal.mapper;

import com.example.autotest.infra.core.mybatis.BaseMapperX;
import com.example.autotest.swagger.dal.dataobject.ProjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapperX<ProjectDO> {
}
