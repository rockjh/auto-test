package com.skyler.autotest.executor.runner;

import com.skyler.autotest.executor.error.ExecutorErrorCode;
import com.skyler.autotest.executor.service.ExecutionLogService;
import com.skyler.autotest.executor.service.dto.ExecutionLogCreateRequest;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.scenario.dal.dataobject.ScenarioStepDO;
import com.skyler.autotest.template.api.TemplateService;
import com.skyler.autotest.template.api.dto.TemplatePreviewRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 单个步骤执行器：负责变量准备、HTTP 调用、结果解析与变量提取。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StepRunner {

    private static final TypeReference<List<VariableMapping>> MAPPING_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<VariableExtractor>> EXTRACTOR_TYPE = new TypeReference<>() {
    };

    private final TemplateService templateService;
    private final RestTemplate restTemplate;
    private final ExecutionLogService executionLogService;
    private final ObjectMapper objectMapper;

    public StepRunResult run(StepRunContext context) {
        ScenarioStepRuntime runtime = buildRuntime(context);
        Map<String, Object> stepVariables = prepareVariables(context, runtime);
        String command = generateCurl(context, stepVariables);
        CurlRequest request = CurlCommandParser.parse(command);

        recordLog(context, "INFO", "步骤执行开始", buildExtra(
                "stepOrder", context.getStepOrder(),
                "method", request.getMethod(),
                "url", request.getUrl()
        ));

        StepRunResult result = executeWithRetry(context, runtime, request, stepVariables);
        if (result.isSuccess()) {
            recordLog(context, "INFO", "步骤执行成功", buildExtra(
                    "status", result.getHttpStatus(),
                    "durationMs", result.getDurationMs()
            ));
        } else {
            recordLog(context, "ERROR", "步骤执行失败", buildExtra(
                    "status", result.getHttpStatus(),
                    "error", result.getErrorMessage()
            ));
        }
        return result;
    }

    private ScenarioStepRuntime buildRuntime(StepRunContext context) {
        ScenarioStepRuntime runtime = new ScenarioStepRuntime();
        runtime.setMappings(parseMappings(context.getStep().getVariableMapping()));
        runtime.setExtractors(parseExtractors(context.getStep().getExtractors()));
        runtime.setOptions(parseOptions(context.getStep().getInvokeOptions()));
        return runtime;
    }

    private Map<String, Object> prepareVariables(StepRunContext context, ScenarioStepRuntime runtime) {
        VariableContext variableContext = context.getVariableContext();
        Map<String, Object> prepared = variableContext.snapshot();
        if (runtime.getMappings().isEmpty()) {
            return prepared;
        }
        for (VariableMapping mapping : runtime.getMappings()) {
            Object value = variableContext.readFromLastResponse(mapping.getSourcePath());
            if (value == null && mapping.getDefaultValue() != null) {
                value = mapping.getDefaultValue();
            }
            if (value != null && StringUtils.isNotBlank(mapping.getTargetKey())) {
                prepared.put(mapping.getTargetKey(), value);
                variableContext.put(mapping.getTargetKey(), value);
            }
        }
        return prepared;
    }

    private String generateCurl(StepRunContext context, Map<String, Object> stepVariables) {
        TemplatePreviewRequest previewRequest = new TemplatePreviewRequest();
        previewRequest.setVariables(stepVariables);
        try {
            return templateService.preview(context.getStep().getCurlVariantId(), previewRequest);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ExecutorErrorCode.TEMPLATE_PREVIEW_FAILED, ex.getMessage(), ex);
        }
    }

    private StepRunResult executeWithRetry(StepRunContext context,
                                           ScenarioStepRuntime runtime,
                                           CurlRequest request,
                                           Map<String, Object> variables) {
        int maxAttempts = Math.max(runtime.getOptions().getMaxAttempts(), 1);
        long backoff = Math.max(runtime.getOptions().getBackoffMs(), 0);
        StepRunResult lastResult = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                recordLog(context, "INFO", "HTTP 调用准备", buildExtra(
                        "attempt", attempt,
                        "headers", request.getHeaders()
                ));
                ResponseTuple tuple = executeHttp(request);
                long duration = System.currentTimeMillis() - start;
                StepRunResult result = buildSuccessResult(tuple, variables, context, runtime, duration);
                return result;
            } catch (RestClientResponseException restEx) {
                long duration = System.currentTimeMillis() - start;
                StepRunResult failed = buildFailureResult(context, request, restEx.getRawStatusCode(),
                        restEx.getResponseBodyAsString(), restEx.getMessage(), duration);
                lastResult = failed;
                recordLog(context, "ERROR", "HTTP 响应异常", buildExtra(
                        "attempt", attempt,
                        "status", restEx.getRawStatusCode(),
                        "error", restEx.getMessage()
                ));
                if (attempt < maxAttempts && backoff > 0) {
                    sleepSilently(backoff);
                }
            } catch (Exception ex) {
                long duration = System.currentTimeMillis() - start;
                StepRunResult failed = buildFailureResult(context, request, 0, null, ex.getMessage(), duration);
                lastResult = failed;
                recordLog(context, "ERROR", "HTTP 调用失败", buildExtra(
                        "attempt", attempt,
                        "error", ex.getMessage()
                ));
                if (attempt < maxAttempts && backoff > 0) {
                    sleepSilently(backoff);
                }
            }
        }
        return lastResult != null ? lastResult : StepRunResult.failed("unknown error");
    }

    private ResponseTuple executeHttp(CurlRequest request) {
        HttpMethod method = resolveHttpMethod(request.getMethod());
        if (method == null) {
            method = HttpMethod.GET;
        }
        HttpHeaders headers = new HttpHeaders();
        request.getHeaders().forEach(headers::add);
        if (request.getBody() != null && !headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        }
        HttpEntity<String> entity = new HttpEntity<>(request.getBody(), headers);
        ResponseEntity<String> response = restTemplate.exchange(request.getUrl(), method, entity, String.class);
        return new ResponseTuple(request, response);
    }

    private HttpMethod resolveHttpMethod(String method) {
        if (StringUtils.isBlank(method)) {
            return null;
        }
        try {
            return HttpMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private StepRunResult buildSuccessResult(ResponseTuple tuple,
                                             Map<String, Object> variables,
                                             StepRunContext context,
                                             ScenarioStepRuntime runtime,
                                             long durationMs) {
        StepRunResult result = StepRunResult.success();
        result.setDurationMs(durationMs);
        result.setHttpStatus(tuple.response().getStatusCodeValue());
        result.setRequestSnapshot(writeJson(buildRequestSnapshot(tuple.request())));
        result.setResponseSnapshot(writeJson(buildResponseSnapshot(tuple.response(), durationMs)));

        context.getVariableContext().updateLastResponse(tuple.response().getBody());
        applyExtractors(runtime, context);
        result.setVariablesSnapshot(writeJson(context.getVariableContext().snapshot()));
        return result;
    }

    private void applyExtractors(ScenarioStepRuntime runtime, StepRunContext context) {
        if (runtime.getExtractors().isEmpty()) {
            return;
        }
        Object lastResponse = context.getVariableContext().getLastResponseJson();
        if (lastResponse == null) {
            return;
        }
        for (VariableExtractor extractor : runtime.getExtractors()) {
            try {
                Object value = JsonPath.read(lastResponse, extractor.getJsonPath());
                if (value instanceof List<?> list) {
                    if (list.isEmpty()) {
                        continue;
                    }
                    value = list.get(0);
                }
                if (value != null && StringUtils.isNotBlank(extractor.getTargetKey())) {
                    context.getVariableContext().put(extractor.getTargetKey(), value);
                }
            } catch (PathNotFoundException ex) {
                log.debug("extractor path not found: {}", extractor.getJsonPath());
            }
        }
    }

    private StepRunResult buildFailureResult(StepRunContext context,
                                             CurlRequest request,
                                             int statusCode,
                                             String responseBody,
                                             String errorMessage,
                                             long durationMs) {
        StepRunResult failed = StepRunResult.failed(errorMessage);
        failed.setDurationMs(durationMs);
        failed.setHttpStatus(statusCode);
        failed.setRequestSnapshot(writeJson(buildRequestSnapshot(request)));
        if (responseBody != null) {
            Map<String, Object> responseSnapshot = new HashMap<>();
            responseSnapshot.put("status", statusCode);
            responseSnapshot.put("body", responseBody);
            responseSnapshot.put("durationMs", durationMs);
            failed.setResponseSnapshot(writeJson(responseSnapshot));
        }
        failed.setVariablesSnapshot(writeJson(context.getVariableContext().snapshot()));
        return failed;
    }

    private Map<String, Object> buildRequestSnapshot(CurlRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("method", request.getMethod());
        snapshot.put("url", request.getUrl());
        snapshot.put("headers", request.getHeaders());
        snapshot.put("body", request.getBody());
        return snapshot;
    }

    private Map<String, Object> buildResponseSnapshot(ResponseEntity<String> response, long durationMs) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", response.getStatusCode().value());
        snapshot.put("headers", response.getHeaders().toSingleValueMap());
        snapshot.put("body", response.getBody());
        snapshot.put("durationMs", durationMs);
        return snapshot;
    }

    private void recordLog(StepRunContext context, String level, String message, Map<String, Object> extra) {
        ExecutionLogCreateRequest logRequest = new ExecutionLogCreateRequest();
        logRequest.setExecutionId(context.getExecutionId());
        logRequest.setScenarioStepId(context.getStep().getId());
        logRequest.setLevel(level);
        logRequest.setMessage(message);
        logRequest.setExtra(extra);
        executionLogService.appendLog(context.getTenantId(), context.getProjectId(), logRequest);
    }

    private Map<String, Object> buildExtra(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (keyValues == null) {
            return map;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                map.put(String.valueOf(key), value);
            }
        }
        return map;
    }

    private StepInvokeOptions parseOptions(String invokeOptions) {
        StepInvokeOptions options = new StepInvokeOptions();
        if (StringUtils.isBlank(invokeOptions)) {
            return options;
        }
        try {
            JsonNode node = objectMapper.readTree(invokeOptions);
            if (node == null) {
                return options;
            }
            if (node.isInt()) {
                options.setMaxAttempts(node.intValue());
            }
            if (node.has("retry")) {
                JsonNode retryNode = node.get("retry");
                if (retryNode.isInt()) {
                    options.setMaxAttempts(retryNode.intValue());
                } else if (retryNode.isObject()) {
                    if (retryNode.has("maxAttempts")) {
                        options.setMaxAttempts(retryNode.get("maxAttempts").asInt(options.getMaxAttempts()));
                    }
                    if (retryNode.has("backoffMs")) {
                        options.setBackoffMs(retryNode.get("backoffMs").asLong(options.getBackoffMs()));
                    }
                }
            }
            if (node.has("maxAttempts")) {
                options.setMaxAttempts(node.get("maxAttempts").asInt(options.getMaxAttempts()));
            }
            if (node.has("backoffMs")) {
                options.setBackoffMs(node.get("backoffMs").asLong(options.getBackoffMs()));
            }
        } catch (JsonProcessingException ignored) {
            log.warn("invalid invoke options json: {}", invokeOptions);
        }
        return options;
    }

    private List<VariableMapping> parseMappings(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, MAPPING_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("invalid variable mapping json: {}", json);
            return List.of();
        }
    }

    private List<VariableExtractor> parseExtractors(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, EXTRACTOR_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("invalid extractor json: {}", json);
            return List.of();
        }
    }

    private String writeJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void sleepSilently(long backoff) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoff);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 步骤执行入参。
     */
    @Data
    public static class StepRunContext {
        private final Long tenantId;
        private final Long projectId;
        private final Long executionId;
        private final int stepOrder;
        private final ScenarioStepDO step;
        private final VariableContext variableContext;
    }

    /**
     * 步骤执行结果。
     */
    @Data
    public static class StepRunResult {
        private boolean success;
        private String requestSnapshot;
        private String responseSnapshot;
        private String variablesSnapshot;
        private String errorMessage;
        private int httpStatus;
        private long durationMs;

        public static StepRunResult success() {
            StepRunResult result = new StepRunResult();
            result.setSuccess(true);
            return result;
        }

        public static StepRunResult failed(String message) {
            StepRunResult result = new StepRunResult();
            result.setSuccess(false);
            result.setErrorMessage(message);
            return result;
        }
    }

    /**
     * 步骤运行期配置的聚合模型。
     */
    @Data
    private static class ScenarioStepRuntime {
        private List<VariableMapping> mappings = List.of();
        private List<VariableExtractor> extractors = List.of();
        private StepInvokeOptions options = new StepInvokeOptions();
    }

    /**
     * 变量映射配置。
     */
    @Data
    private static class VariableMapping {
        private String sourcePath;
        private String targetKey;
        private Object defaultValue;
    }

    /**
     * 变量提取配置。
     */
    @Data
    private static class VariableExtractor {
        private String jsonPath;
        private String targetKey;
    }

    /**
     * 步骤调用选项，如重试策略等。
     */
    @Data
    private static class StepInvokeOptions {
        private int maxAttempts = 1;
        private long backoffMs = 1000;
    }

    /**
     * 解析后的 curl 请求结构体。
     */
    @Data
    private static class CurlRequest {
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;
    }

    private record ResponseTuple(CurlRequest request, ResponseEntity<String> response) {
    }

    /**
     * 轻量级的 curl 命令解析器，仅支持常见参数组合。
     */
    private static final class CurlCommandParser {

        private static CurlRequest parse(String command) {
            if (StringUtils.isBlank(command)) {
                throw new BizException(ExecutorErrorCode.CURL_COMMAND_EMPTY);
            }
            List<String> tokens = tokenize(command);
            if (tokens.isEmpty() || !"curl".equalsIgnoreCase(tokens.get(0))) {
                throw new BizException(ExecutorErrorCode.CURL_COMMAND_INVALID);
            }
            String method = "GET";
            String url = null;
            Map<String, String> headers = new LinkedHashMap<>();
            StringBuilder bodyBuilder = new StringBuilder();
            boolean hasBody = false;
            for (int i = 1; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if ("-X".equals(token) || "--request".equals(token)) {
                    method = tokens.get(++i).toUpperCase();
                } else if ("-H".equals(token) || "--header".equals(token)) {
                    String headerLine = tokens.get(++i);
                    int idx = headerLine.indexOf(':');
                    if (idx > 0) {
                        String name = headerLine.substring(0, idx).trim();
                        String value = headerLine.substring(idx + 1).trim();
                        headers.put(name, value);
                    }
                } else if (token.startsWith("-")) {
                    if (token.startsWith("-d") || token.startsWith("--data")) {
                        String data;
                        if (token.contains("=")) {
                            data = token.substring(token.indexOf('=') + 1);
                        } else {
                            data = tokens.get(++i);
                        }
                        if (bodyBuilder.length() > 0) {
                            bodyBuilder.append('&');
                        }
                        bodyBuilder.append(data);
                        hasBody = true;
                    }
                } else {
                    if (url == null) {
                        url = token;
                    }
                }
            }
            if (url == null) {
                throw new BizException(ExecutorErrorCode.CURL_URL_MISSING);
            }
            if (hasBody && "GET".equalsIgnoreCase(method)) {
                method = "POST";
            }
            String body = hasBody ? bodyBuilder.toString() : null;
            return new CurlRequest(method.toUpperCase(), url, headers, body);
        }

        private static List<String> tokenize(String command) {
            List<String> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inSingleQuote = false;
            boolean inDoubleQuote = false;
            for (int i = 0; i < command.length(); i++) {
                char c = command.charAt(i);
                if (c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                    continue;
                }
                if (c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                    continue;
                }
                if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    continue;
                }
                if (c == '\\' && i + 1 < command.length()) {
                    current.append(command.charAt(++i));
                    continue;
                }
                current.append(c);
            }
            if (current.length() > 0) {
                tokens.add(current.toString());
            }
            return tokens;
        }
    }
}
