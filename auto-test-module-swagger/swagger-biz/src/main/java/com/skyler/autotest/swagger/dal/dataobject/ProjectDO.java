package com.skyler.autotest.swagger.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_project")
public class ProjectDO extends BaseDO {

    private String name;
    private String swaggerSource;
    private Integer swaggerType;
    private String swaggerVersion;
    private String swaggerHash;
    private Integer syncStatus;
    private java.time.LocalDateTime syncTime;
    private String tags;
    private Integer status;
    private String remark;
    @TableField("extra")
    private String extra;
}
