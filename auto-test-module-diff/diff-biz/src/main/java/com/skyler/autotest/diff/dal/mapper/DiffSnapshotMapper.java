package com.skyler.autotest.diff.dal.mapper;

import com.skyler.autotest.diff.dal.dataobject.DiffSnapshotDO;
import com.skyler.autotest.infra.core.mybatis.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 差异快照 Mapper，继承基础 Mapper 提供 CRUD 能力。
 */
@Mapper
public interface DiffSnapshotMapper extends BaseMapperX<DiffSnapshotDO> {
}
