package com.skyler.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

/**
 * 场景变量数据对象，对应表 {@code autotest_scenario_variable}，描述变量定义与提取规则。
 */
@Data
@TableName("autotest_scenario_variable")
public class ScenarioVariableDO extends BaseDO {

    /** 关联场景编号。 */
    private Long scenarioId;

    /** 变量作用域，scenario 或 step。 */
    private String scope;

    /** 所属步骤编号，仅作用域为 step 时生效。 */
    private Long ownerStepId;

    /** 变量名称。 */
    private String varName;

    /** 变量来源类型。 */
    private String sourceType;

    /** 初始值配置。 */
    private String initValue;

    /** 变量绑定配置 JSON。 */
    private String bindingConfig;

    /** 备注信息。 */
    private String remark;
}
