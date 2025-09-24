package com.example.autotest.swagger.dal.mapper;

import com.example.autotest.infra.core.mybatis.BaseMapperX;
import com.example.autotest.swagger.dal.dataobject.SwaggerSyncDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SwaggerSyncMapper extends BaseMapperX<SwaggerSyncDO> {
}
