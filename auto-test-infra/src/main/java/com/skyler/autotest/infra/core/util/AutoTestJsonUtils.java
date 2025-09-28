package com.skyler.autotest.infra.core.util;

import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 自动化测试域专用的 JSON 序列化工具，基于 Spring 统一注入的 {@link ObjectMapper} 提供异常包装能力。
 */
@Component
public class AutoTestJsonUtils {

    private final ObjectMapper objectMapper;

    public AutoTestJsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 序列化对象为 JSON 字符串，失败时抛出带统一错误码的业务异常，便于上层感知。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw AutoTestErrorCode.JSON_SERIALIZE_FAILED.exception(ex);
        }
    }
}
