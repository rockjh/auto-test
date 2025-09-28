package com.skyler.autotest.swagger.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_group")
public class GroupDO extends BaseDO {
    private Long collectionId;
    private Long projectId;
    private String method;
    private String path;
    private String summary;
    private String operationId;
    private String hash;
    private Boolean deprecated;
    private Integer status;
    private String requestSchema;
    private String responseSchema;
    private Long lastSyncId;
    private String remark;
}
