package com.skyler.autotest.infra.core.error;

import com.skyler.autotest.infra.core.exception.BizException;

/**
 * 统一错误码抽象，所有业务异常均应实现该接口，确保错误码与描述集中管理。
 */
public interface ErrorCode {

    /**
     * 返回错误码数值。
     *
     * @return 错误码
     */
    int getCode();

    /**
     * 返回错误码默认描述。
     *
     * @return 默认错误信息
     */
    String getMessage();

    /**
     * 基于当前错误码构造 {@link BizException}。
     *
     * @return 业务异常
     */
    default BizException exception() {
        return new BizException(this);
    }

    /**
     * 基于当前错误码构造 {@link BizException}，并覆盖默认描述。
     *
     * @param message 自定义异常描述
     * @return 业务异常
     */
    default BizException exception(String message) {
        return new BizException(this, message);
    }

    /**
     * 基于当前错误码构造 {@link BizException}，同时保留底层异常。
     *
     * @param cause 原始异常
     * @return 业务异常
     */
    default BizException exception(Throwable cause) {
        return new BizException(this, cause);
    }
}
