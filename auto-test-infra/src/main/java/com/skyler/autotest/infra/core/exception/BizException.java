package com.skyler.autotest.infra.core.exception;

import com.skyler.autotest.infra.core.error.ErrorCode;
import org.springframework.lang.Nullable;

/**
 * 业务异常，携带统一错误码，供全局异常处理与前端提示使用。
 */
public class BizException extends RuntimeException {

    private final int code;
    @Nullable
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 为兼容旧逻辑保留的构造方法，后续应逐步迁移到 {@link ErrorCode} 枚举。
     */
    @Deprecated
    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
