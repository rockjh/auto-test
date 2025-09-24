package com.example.autotest.swagger.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_collection")
public class CollectionDO extends BaseDO {
    private Long projectId;
    private String tag;
    private String summary;
    private Integer orderNo;
    private Integer status;
    private String metadata;
}
