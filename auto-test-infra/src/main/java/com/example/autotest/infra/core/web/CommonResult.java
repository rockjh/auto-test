package com.example.autotest.infra.core.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用接口响应包装，配合 {@code CommonResult#success}/{@code CommonResult#failure} 快速构造标准返回体。
 *
 * @param <T> 实际负载类型
 */
@Data
@Schema(description = "通用接口响应包装")
public class CommonResult<T> {

    @Schema(description = "业务状态码，0 表示成功", example = "0")
    private Integer code;

    @Schema(description = "状态描述信息", example = "OK")
    private String message;

    @Schema(description = "具体业务数据")
    private T data;

    /**
     * 构造成功响应。
     *
     * @param data 成功结果数据
     * @param <T>  数据类型
     * @return 包装后的成功响应
     */
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(0);
        result.setMessage("OK");
        result.setData(data);
        return result;
    }

    /**
     * 构造失败响应。
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 包装后的失败响应
     */
    public static <T> CommonResult<T> failure(int code, String message) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
