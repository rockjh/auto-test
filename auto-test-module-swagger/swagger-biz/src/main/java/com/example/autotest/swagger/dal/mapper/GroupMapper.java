package com.example.autotest.swagger.dal.mapper;

import com.example.autotest.infra.core.mybatis.BaseMapperX;
import com.example.autotest.swagger.dal.dataobject.GroupDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapperX<GroupDO> {
}
