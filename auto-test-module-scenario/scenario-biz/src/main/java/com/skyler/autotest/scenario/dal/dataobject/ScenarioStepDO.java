package com.skyler.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

/**
 * 场景步骤数据对象，对应表 {@code autotest_scenario_step}，保存步骤定义及变量映射。
 */
@Data
@TableName("autotest_scenario_step")
public class ScenarioStepDO extends BaseDO {

    /** 关联场景编号。 */
    private Long scenarioId;

    /** 执行顺序，从 1 开始递增。 */
    private Integer orderNo;

    /** 步骤别名。 */
    private String stepAlias;

    /** 引用的 curl 模板编号。 */
    private Long curlVariantId;

    /** 调用选项 JSON，如重试次数等。 */
    private String invokeOptions;

    /** 变量映射配置 JSON。 */
    private String variableMapping;

    /** 变量提取器配置 JSON。 */
    private String extractors;

    /** 条件执行配置 JSON。 */
    private String conditionConfig;

    /** 备注信息。 */
    private String remark;
}
