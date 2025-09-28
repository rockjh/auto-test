package com.skyler.autotest.executor.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护场景执行过程中的变量上下文，包括环境变量、提取出来的变量以及最近一次响应。
 */
@Slf4j
public class VariableContext {

    private final Map<String, Object> variables = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;
    private Object lastResponseJson;

    public VariableContext(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void putAll(Map<String, Object> source) {
        if (source != null) {
            variables.putAll(source);
        }
    }

    public Map<String, Object> snapshot() {
        return new LinkedHashMap<>(variables);
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public void put(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        variables.put(key, value);
    }

    public Object readFromLastResponse(String jsonPath) {
        if (lastResponseJson == null || jsonPath == null) {
            return null;
        }
        try {
            Object value = JsonPath.read(lastResponseJson, jsonPath);
            if (value instanceof List<?> list) {
                return list.isEmpty() ? null : list.get(0);
            }
            return value;
        } catch (PathNotFoundException ex) {
            log.debug("json path not found: {}", jsonPath);
            return null;
        }
    }

    public void updateLastResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            this.lastResponseJson = null;
            return;
        }
        try {
            this.lastResponseJson = objectMapper.readValue(responseBody, Object.class);
        } catch (Exception ex) {
            log.debug("response is not json: {}", ex.getMessage());
            this.lastResponseJson = null;
        }
    }

    public Object getLastResponseJson() {
        return lastResponseJson;
    }
}
