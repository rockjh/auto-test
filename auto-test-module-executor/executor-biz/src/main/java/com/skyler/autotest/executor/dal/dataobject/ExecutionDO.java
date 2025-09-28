package com.skyler.autotest.executor.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场景执行记录，对应表 {@code autotest_execution}，记录执行入口与整体状态。
 */
@Data
@TableName("autotest_execution")
public class ExecutionDO extends BaseDO {

    /** 场景编号。 */
    private Long scenarioId;

    /** 项目编号。 */
    private Long projectId;

    /** 使用的环境编号。 */
    private Long envId;

    /** 触发类型：manual/webhook 等。 */
    private String triggerType;

    /** 触发人。 */
    private String triggerUser;

    /** 外部任务关联 ID。 */
    private String externalJobId;

    /** 执行状态，参见 {@link com.skyler.autotest.executor.enums.ExecutionStatus}。 */
    private Integer status;

    /** 开始时间。 */
    private LocalDateTime startTime;

    /** 结束时间。 */
    private LocalDateTime endTime;

    /** 耗时毫秒。 */
    private Long durationMs;

    /** 执行概要或失败原因。 */
    private String summary;

    /** 通知渠道配置。 */
    private String notifyChannels;

    /** 备注信息。 */
    private String remark;
}
