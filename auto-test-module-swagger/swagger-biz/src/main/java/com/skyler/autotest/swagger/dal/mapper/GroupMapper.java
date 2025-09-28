package com.skyler.autotest.swagger.dal.mapper;

import com.skyler.autotest.infra.core.mybatis.BaseMapperX;
import com.skyler.autotest.swagger.dal.dataobject.GroupDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapperX<GroupDO> {
}
