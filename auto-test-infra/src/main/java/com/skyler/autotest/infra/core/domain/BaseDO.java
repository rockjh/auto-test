package com.skyler.autotest.infra.core.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领域模型基础类，封装所有主表公共字段。
 */
@Data
@TableName(autoResultMap = true)
public abstract class BaseDO {

    /** 主键。 */
    @TableId
    private Long id;

    /** 租户编号。 */
    private Long tenantId;

    /** 创建人。 */
    @TableField(fill = FieldFill.INSERT)
    private String creator;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记。 */
    @TableLogic
    private Boolean deleted;
}
