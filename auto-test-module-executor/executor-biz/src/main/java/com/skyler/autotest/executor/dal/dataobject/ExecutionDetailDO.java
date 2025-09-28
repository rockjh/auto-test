package com.skyler.autotest.executor.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.skyler.autotest.infra.core.domain.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行步骤详情，对应表 {@code autotest_execution_detail}，记录每个步骤的调用信息。
 */
@Data
@TableName("autotest_execution_detail")
public class ExecutionDetailDO extends BaseDO {

    /** 关联执行记录编号。 */
    private Long executionId;

    /** 场景步骤编号。 */
    private Long scenarioStepId;

    /** 步骤顺序。 */
    private Integer stepOrder;

    /** 步骤状态，参见 {@link com.skyler.autotest.executor.enums.ExecutionStepStatus}。 */
    private Integer status;

    /** 重试次数。 */
    private Integer retryCount;

    /** 请求快照。 */
    private String requestSnapshot;

    /** 响应快照。 */
    private String responseSnapshot;

    /** 变量快照。 */
    private String variablesSnapshot;

    /** 错误信息。 */
    private String errorMessage;

    /** 步骤开始时间。 */
    private LocalDateTime startTime;

    /** 步骤结束时间。 */
    private LocalDateTime endTime;

    /** 备注信息。 */
    private String remark;
}
