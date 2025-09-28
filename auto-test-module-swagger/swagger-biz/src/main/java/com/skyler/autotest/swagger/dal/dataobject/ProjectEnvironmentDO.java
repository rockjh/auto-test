package com.skyler.autotest.swagger.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_project_env")
public class ProjectEnvironmentDO extends BaseDO {

    private Long projectId;
    private String name;
    private Integer envType;
    private String host;
    private String headers;
    private String variables;
    private Boolean isDefault;
    private Integer status;
    private String remark;
}
