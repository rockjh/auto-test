package com.skyler.autotest.infra.core.web;

import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import com.skyler.autotest.infra.core.error.ErrorCode;
import com.skyler.autotest.infra.core.exception.BizException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一转换为 {@link CommonResult} 返回体。
 */
@RestControllerAdvice(basePackages = "com.skyler.autotest")
public class AutoTestGlobalExceptionHandler {

    /**
     * 捕获业务异常，返回统一错误结构，方便前端识别。
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleBizException(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode != null) {
            return CommonResult.failure(errorCode, ex.getMessage());
        }
        return CommonResult.failure(ex.getCode(), ex.getMessage());
    }

    /**
     * 捕获未知异常，统一返回内部错误，具体详情依赖日志排查。
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleThrowable(Throwable ex) {
        return CommonResult.failure(AutoTestErrorCode.INTERNAL_ERROR, ex.getMessage());
    }
}
