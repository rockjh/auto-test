package com.skyler.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

/**
 * 场景数据对象，对应表 {@code autotest_scenario}，承载场景基础信息。
 */
@Data
@TableName("autotest_scenario")
public class ScenarioDO extends BaseDO {

    /** 所属项目编号。 */
    private Long projectId;

    /** 场景名称。 */
    private String name;

    /** 场景状态，参见 {@link com.skyler.autotest.scenario.enums.ScenarioStatus}。 */
    private Integer status;

    /** 标签集合，逗号分隔。 */
    private String tags;

    /** 负责人账号编号。 */
    private Long ownerId;

    /** 负责人姓名。 */
    private String ownerName;

    /** 默认执行环境编号。 */
    private Long defaultEnvId;

    /** 场景元数据 JSON。 */
    private String metadata;

    /** 是否需要人工复核。 */
    private Boolean needReview;

    /** 最近一次执行记录编号。 */
    private Long lastExecutionId;

    /** 备注信息。 */
    private String remark;
}
