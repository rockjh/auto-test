package com.skyler.autotest.scenario.api.util;

import com.skyler.autotest.scenario.api.model.ScenarioVariableMappingRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 变量映射解析工具，支持 map 或数组两种 JSON 结构，兜底返回解析状态。
 */
public final class ScenarioVariableMappingParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ScenarioVariableMappingRule>> LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    private ScenarioVariableMappingParser() {
    }

    public static ParseResult parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return ParseResult.success(Collections.emptyList(), "[]", false);
        }
        String trimmed = rawJson.trim();
        try {
            List<ScenarioVariableMappingRule> rules;
            if (trimmed.startsWith("[")) {
                rules = MAPPER.readValue(trimmed, LIST_TYPE);
            } else {
                Map<String, String> asMap = MAPPER.readValue(trimmed, MAP_TYPE);
                rules = new ArrayList<>(asMap.size());
                asMap.forEach((target, source) -> {
                    ScenarioVariableMappingRule rule = new ScenarioVariableMappingRule();
                    rule.setTargetKey(target);
                    rule.setSourcePath(source);
                    rules.add(rule);
                });
            }
            List<ScenarioVariableMappingRule> normalized = new ArrayList<>(rules.size());
            boolean hasIncompleteRule = false;
            for (ScenarioVariableMappingRule rule : rules) {
                if (rule == null) {
                    hasIncompleteRule = true;
                    continue;
                }
                if (rule.getSourcePath() == null || rule.getSourcePath().isBlank()
                        || rule.getTargetKey() == null || rule.getTargetKey().isBlank()) {
                    hasIncompleteRule = true;
                    continue;
                }
                ScenarioVariableMappingRule cleaned = new ScenarioVariableMappingRule();
                cleaned.setSourcePath(rule.getSourcePath());
                cleaned.setTargetKey(rule.getTargetKey());
                cleaned.setRemark(rule.getRemark());
                normalized.add(cleaned);
            }
            String normalizedJson = MAPPER.writeValueAsString(normalized);
            return ParseResult.success(Collections.unmodifiableList(normalized), normalizedJson, hasIncompleteRule);
        } catch (JsonProcessingException ex) {
            return ParseResult.failure(trimmed, ex.getOriginalMessage());
        }
    }

    public static final class ParseResult {
        private final List<ScenarioVariableMappingRule> rules;
        private final String normalizedJson;
        private final boolean success;
        private final boolean hasIncompleteRule;
        private final String errorMessage;
        private final String rawJson;

        private ParseResult(List<ScenarioVariableMappingRule> rules,
                            String normalizedJson,
                            boolean success,
                            boolean hasIncompleteRule,
                            String errorMessage,
                            String rawJson) {
            this.rules = rules;
            this.normalizedJson = normalizedJson;
            this.success = success;
            this.hasIncompleteRule = hasIncompleteRule;
            this.errorMessage = errorMessage;
            this.rawJson = rawJson;
        }

        public static ParseResult success(List<ScenarioVariableMappingRule> rules,
                                          String normalizedJson,
                                          boolean hasIncompleteRule) {
            return new ParseResult(rules,
                    normalizedJson,
                    true,
                    hasIncompleteRule,
                    null,
                    normalizedJson);
        }

        public static ParseResult failure(String rawJson, String errorMessage) {
            return new ParseResult(Collections.emptyList(), rawJson, false, true, errorMessage, rawJson);
        }

        public List<ScenarioVariableMappingRule> getRules() {
            return rules;
        }

        public String getNormalizedJson() {
            return normalizedJson;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean hasIncompleteRule() {
            return hasIncompleteRule;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getRawJson() {
            return rawJson;
        }
    }
}
