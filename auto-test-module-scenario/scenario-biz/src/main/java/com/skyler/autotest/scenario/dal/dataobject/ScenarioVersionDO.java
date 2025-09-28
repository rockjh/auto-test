package com.skyler.autotest.scenario.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

/**
 * 场景版本快照，对应表 {@code autotest_scenario_version}，保存发布时的字段快照。
 */
@Data
@TableName("autotest_scenario_version")
public class ScenarioVersionDO extends BaseDO {

    /** 关联场景编号。 */
    private Long scenarioId;

    /** 版本号，例如 v20250115。 */
    private String versionNo;

    /** 快照内容 JSON。 */
    private String content;

    /** 发布备注。 */
    private String comment;

    /** 发布人编号。 */
    private Long publisherId;

    /** 发布人姓名。 */
    private String publisherName;
}
