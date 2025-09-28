package com.skyler.autotest.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.infra.event.TemplateUpdateEvent;
import com.skyler.autotest.template.api.enums.TemplateChangeType;
import com.skyler.autotest.template.api.enums.TemplateVariantType;
import com.skyler.autotest.template.dal.dataobject.CurlVariantDO;
import com.skyler.autotest.template.dal.mapper.CurlVariantMapper;
import com.skyler.autotest.swagger.dal.dataobject.GroupDO;
import com.skyler.autotest.swagger.dal.dataobject.ProjectEnvironmentDO;
import com.skyler.autotest.swagger.dal.mapper.GroupMapper;
import com.skyler.autotest.swagger.dal.mapper.ProjectEnvironmentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.skyler.autotest.template.api.constant.TemplateConstants.ACCEPT_HEADER;
import static com.skyler.autotest.template.api.constant.TemplateConstants.BODY_SEGMENT_PREFIX;
import static com.skyler.autotest.template.api.constant.TemplateConstants.BODY_SEGMENT_TEMPLATE;
import static com.skyler.autotest.template.api.constant.TemplateConstants.BODY_VARIABLE_KEY;
import static com.skyler.autotest.template.api.constant.TemplateConstants.CONTENT_TYPE_HEADER;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_ACCEPT_SEGMENT;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_CONTENT_TYPE_SEGMENT;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_SAMPLE_SUFFIX;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_HOST_FALLBACK;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_HTTP_METHOD;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HEADER_PREFIX;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HEADER_SEGMENT_TEMPLATE;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HOST_PLACEHOLDER;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HOST_VARIABLE_KEY;
import static com.skyler.autotest.template.api.constant.TemplateConstants.PATH_PREFIX;
import static com.skyler.autotest.template.api.constant.TemplateConstants.QUERY_PREFIX;

/**
 * 根据最新 Swagger 元数据生成或更新 curl 模板，并发布模板变更事件。
 */
@Service
@RequiredArgsConstructor
public class TemplateGenerationService {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {
    };

    private final CurlVariantMapper curlVariantMapper;
    private final GroupMapper groupMapper;
    private final ProjectEnvironmentMapper projectEnvironmentMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final EnumMap<TemplateVariantType, TemplateVariantBuilder> variantBuilders =
            new EnumMap<>(TemplateVariantType.class);

    @PostConstruct
    public void registerBuilders() {
        variantBuilders.put(TemplateVariantType.MINIMAL, new MinimalVariantBuilder());
        variantBuilders.put(TemplateVariantType.FULL, new FullVariantBuilder());
    }

    /**
     * 生成或更新指定接口的最小模板，作为调用 {@link #generateVariant(Long, TemplateVariantType, boolean, TemplateChangeType)} 的便捷方法。
     *
     * @param groupId        接口分组 ID
     * @param markNeedReview 是否需要复核
     * @param changeType     变更类型，空值默认按手动执行处理
     * @return 持久化后的模板 DO
     */
    @Transactional(rollbackFor = Exception.class)
    public CurlVariantDO generateMinimalVariant(Long groupId,
                                                boolean markNeedReview,
                                                TemplateChangeType changeType) {
        return generateVariant(groupId, TemplateVariantType.MINIMAL, markNeedReview, changeType);
    }

    /**
     * 生成或更新指定接口的模板。
     *
     * @param groupId        接口分组 ID
     * @param variantType    模板类型
     * @param markNeedReview 是否需要复核
     * @param changeType     变更类型
     * @return 持久化后的模板 DO
     */
    @Transactional(rollbackFor = Exception.class)
    public CurlVariantDO generateVariant(Long groupId,
                                         TemplateVariantType variantType,
                                         boolean markNeedReview,
                                         TemplateChangeType changeType) {
        GroupDO group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_GROUP_NOT_FOUND);
        }
        TemplateChangeType effectiveChangeType = Optional.ofNullable(changeType)
                .orElse(TemplateChangeType.MANUAL);
        CurlVariantDO variant = curlVariantMapper.selectOne(new LambdaQueryWrapper<CurlVariantDO>()
                .eq(CurlVariantDO::getGroupId, groupId)
                .eq(CurlVariantDO::getVariantType, variantType.getType()));
        boolean create = variant == null;
        if (create) {
            variant = new CurlVariantDO();
            variant.setId(bizIdGenerator.nextId());
            variant.setTenantId(group.getTenantId());
            variant.setProjectId(group.getProjectId());
            variant.setGroupId(groupId);
            variant.setVariantType(variantType.getType());
        }
        variant.setTenantId(group.getTenantId());
        variant.setProjectId(group.getProjectId());
        variant.setGroupId(groupId);
        TemplateContext context = buildContext(group);
        TemplateVariantBuilder builder = Optional.ofNullable(variantBuilders.get(variantType))
                .orElseThrow(() -> new BizException(AutoTestErrorCode.TEMPLATE_GENERATOR_NOT_REGISTERED,
                        "模板类型未注册: " + variantType.getType()));
        TemplateVariantResult result = builder.build(context);
        applyEnvironmentOverrides(context, result);
        variant.setCurlTemplate(result.getCurlTemplate());
        variant.setParamRules(serializeParamRules(result.getParamRules()));
        variant.setVersionNo(group.getHash());
        variant.setRuleVersion(group.getHash());
        variant.setNeedReview(markNeedReview);
        variant.setDeleted(Boolean.FALSE);

        if (create) {
            curlVariantMapper.insert(variant);
        } else {
            curlVariantMapper.updateById(variant);
        }

        publishTemplateEvent(variant, effectiveChangeType, markNeedReview);
        return variant;
    }

    /**
     * 处理接口被删除时模板的清理动作，标记模板需要复核并发布删除事件。
     *
     * @param groupId 接口分组 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleGroupRemoval(Long groupId) {
        List<CurlVariantDO> variants = curlVariantMapper.selectList(new LambdaQueryWrapper<CurlVariantDO>()
                .eq(CurlVariantDO::getGroupId, groupId));
        if (variants.isEmpty()) {
            return;
        }
        for (CurlVariantDO variant : variants) {
            publishTemplateEvent(variant, TemplateChangeType.DELETE, true);
            LambdaUpdateWrapper<CurlVariantDO> wrapper = new LambdaUpdateWrapper<CurlVariantDO>()
                    .eq(CurlVariantDO::getId, variant.getId())
                    .set(CurlVariantDO::getNeedReview, true)
                    .set(CurlVariantDO::getDeleted, true);
            curlVariantMapper.update(null, wrapper);
        }
    }

    private void publishTemplateEvent(CurlVariantDO variant, TemplateChangeType changeType, boolean needReview) {
        TemplateUpdateEvent event = new TemplateUpdateEvent(
                variant.getTenantId(),
                variant.getProjectId(),
                variant.getGroupId(),
                variant.getId(),
                variant.getVariantType(),
                changeType.getCode(),
                variant.getCurlTemplate(),
                variant.getParamRules(),
                needReview
        );
        eventPublisher.publishEvent(event);
    }

    private TemplateContext buildContext(GroupDO group) {
        Operation operation = parseOperation(group.getRequestSchema());
        TemplateEnvironment environment = loadEnvironment(group.getTenantId(), group.getProjectId());
        return new TemplateContext(group, operation, environment);
    }

    private Operation parseOperation(String requestSchema) {
        if (StringUtils.isBlank(requestSchema)) {
            return new Operation();
        }
        try {
            return objectMapper.readValue(requestSchema, Operation.class);
        } catch (Exception ex) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_OPERATION_PARSE_FAILED,
                    "Swagger 请求定义解析失败", ex);
        }
    }

    private void applyEnvironmentOverrides(TemplateContext context, TemplateVariantResult result) {
        TemplateEnvironment environment = context.environment();
        Map<String, Map<String, Object>> rules = result.getParamRules();

        String hostSample = StringUtils.defaultIfBlank(environment.host(), DEFAULT_HOST_FALLBACK);
        rules.put(HOST_VARIABLE_KEY, buildRule("string", hostSample));

        String updatedTemplate = result.getCurlTemplate();
        for (Map.Entry<String, String> headerEntry : environment.headers().entrySet()) {
            String headerKey = headerEntry.getKey();
            String headerValue = StringUtils.defaultIfEmpty(headerEntry.getValue(),
                    headerKey + DEFAULT_SAMPLE_SUFFIX);
            String placeholder = "${" + HEADER_PREFIX + headerKey + "}";
            updatedTemplate = removeDefaultHeaderIfPresent(updatedTemplate, headerKey);
            if (!updatedTemplate.contains(placeholder)) {
                updatedTemplate = updatedTemplate + String.format(HEADER_SEGMENT_TEMPLATE, headerKey, placeholder);
            }
            Map<String, Object> headerRule = rules.computeIfAbsent(HEADER_PREFIX + headerKey,
                    key -> buildRule("string", headerValue));
            headerRule.putIfAbsent("type", "string");
            headerRule.put("sample", headerValue);
        }
        result.setCurlTemplate(updatedTemplate);
    }

    private String removeDefaultHeaderIfPresent(String template, String headerKey) {
        if (StringUtils.isBlank(template) || StringUtils.isBlank(headerKey)) {
            return template;
        }
        if (CONTENT_TYPE_HEADER.equalsIgnoreCase(headerKey)) {
            template = template.replace(DEFAULT_CONTENT_TYPE_SEGMENT, "");
        }
        if (ACCEPT_HEADER.equalsIgnoreCase(headerKey)) {
            template = template.replace(DEFAULT_ACCEPT_SEGMENT, "");
        }
        return template;
    }

    private TemplateEnvironment loadEnvironment(Long tenantId, Long projectId) {
        List<ProjectEnvironmentDO> environments = projectEnvironmentMapper.selectList(new LambdaQueryWrapper<ProjectEnvironmentDO>()
                .eq(ProjectEnvironmentDO::getTenantId, tenantId)
                .eq(ProjectEnvironmentDO::getProjectId, projectId)
                .eq(ProjectEnvironmentDO::getDeleted, Boolean.FALSE));
        if (environments == null || environments.isEmpty()) {
            return new TemplateEnvironment(null, Collections.emptyMap(), Collections.emptyMap());
        }
        ProjectEnvironmentDO active = environments.stream()
                .filter(env -> env.getStatus() == null || env.getStatus() == 1)
                .sorted((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.getIsDefault()), Boolean.TRUE.equals(a.getIsDefault())))
                .findFirst()
                .orElse(environments.get(0));
        Map<String, String> headers = parseHeaders(active.getHeaders());
        Map<String, Object> variables = parseVariables(active.getVariables());
        return new TemplateEnvironment(active.getHost(), headers, variables);
    }

    private Map<String, String> parseHeaders(String headerJson) {
        if (StringUtils.isBlank(headerJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(headerJson, STRING_MAP_TYPE);
        } catch (Exception ex) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_ENV_HEADER_PARSE_FAILED,
                    "模板环境 Header 配置解析失败", ex);
        }
    }

    private Map<String, Object> parseVariables(String variableJson) {
        if (StringUtils.isBlank(variableJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(variableJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_ENV_VARIABLE_PARSE_FAILED,
                    "模板环境变量解析失败", ex);
        }
    }

    private String serializeParamRules(Map<String, Map<String, Object>> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_RULE_SERIALIZE_FAILED,
                    "模板参数规则序列化失败", e);
        }
    }

    private Map<String, Object> buildRule(String type, Object sample) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", type);
        if (sample != null) {
            rule.put("sample", sample);
        }
        return rule;
    }

    private Map<String, Object> buildNumberRule(String type, Number sample, Number minimum, Number maximum) {
        Map<String, Object> rule = buildRule(type, sample);
        if (minimum != null) {
            rule.put("min", minimum);
        }
        if (maximum != null) {
            rule.put("max", maximum);
        }
        return rule;
    }

    private Map<String, Object> buildBooleanRule(Boolean sample) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", "boolean");
        if (sample != null) {
            rule.put("sample", sample);
        }
        return rule;
    }

    private void attachEnum(Map<String, Object> rule, Schema<?> schema) {
        if (schema == null || schema.getEnum() == null || schema.getEnum().isEmpty()) {
            return;
        }
        rule.put("enum", new ArrayList<>(schema.getEnum()));
        if (!rule.containsKey("sample")) {
            rule.put("sample", schema.getEnum().get(0));
        }
    }

    private TemplateVariantResult buildBaseResult(TemplateContext context) {
        Map<String, Map<String, Object>> rules = new LinkedHashMap<>();
        String curl = buildCurlPrefix(context.group(), context.operation(), rules);
        return new TemplateVariantResult(curl, rules);
    }

    private String buildCurlPrefix(GroupDO group, Operation operation, Map<String, Map<String, Object>> rules) {
        String path = StringUtils.defaultString(group.getPath());
        Map<String, Parameter> pathParameters = collectParameters(operation, "path");
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String param = matcher.group(1);
            Parameter parameter = pathParameters.get(param);
            Map<String, Object> rule = parameter != null
                    ? buildRuleForParameter(parameter)
                    : buildRule("string", param + DEFAULT_SAMPLE_SUFFIX);
            rule.put("required", Boolean.TRUE);
            rules.put(PATH_PREFIX + param, rule);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("{" + param + "}"));
        }
        matcher.appendTail(buffer);
        pathParameters.forEach((name, parameter) ->
                rules.putIfAbsent(PATH_PREFIX + name, buildRuleForParameter(parameter)));

        StringBuilder builder = new StringBuilder();
        builder.append("curl -X ")
                .append(StringUtils.defaultIfBlank(group.getMethod(), DEFAULT_HTTP_METHOD))
                .append(' ')
                .append('"')
                .append(HOST_PLACEHOLDER)
                .append(buffer)
                .append('"');
        return builder.toString();
    }

    private Map<String, Parameter> collectParameters(Operation operation, String inType) {
        if (operation == null || operation.getParameters() == null) {
            return Collections.emptyMap();
        }
        return operation.getParameters().stream()
                .filter(parameter -> StringUtils.equalsIgnoreCase(parameter.getIn(), inType))
                .collect(Collectors.toMap(Parameter::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, Object> buildRuleForParameter(Parameter parameter) {
        Schema<?> schema = resolveParameterSchema(parameter);
        Map<String, Object> rule = createRuleWithSample(parameter.getName(), schema);
        rule.put("required", Boolean.TRUE.equals(parameter.getRequired()));
        if (parameter.getDescription() != null) {
            rule.put("description", parameter.getDescription());
        }
        if (parameter.getExample() != null) {
            rule.put("sample", parameter.getExample());
        }
        return rule;
    }

    private Schema<?> resolveParameterSchema(Parameter parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter.getSchema() != null) {
            return parameter.getSchema();
        }
        if (parameter.getContent() != null && !parameter.getContent().isEmpty()) {
            MediaType mediaType = parameter.getContent().values().iterator().next();
            return mediaType != null ? mediaType.getSchema() : null;
        }
        return null;
    }

    private Map<String, Object> buildStringRule(String name, Schema<?> schema) {
        Object sample = Optional.ofNullable(schema)
                .map(candidate -> {
                    if (candidate.getExample() != null) {
                        return candidate.getExample();
                    }
                    if (candidate.getEnum() != null && !candidate.getEnum().isEmpty()) {
                        return candidate.getEnum().get(0);
                    }
                    if (candidate.getDefault() != null) {
                        return candidate.getDefault();
                    }
                    return name + "-sample";
                })
                .orElse(name + "-sample");
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", "string");
        rule.put("sample", sample);
        if (schema != null) {
            if (schema.getPattern() != null) {
                rule.put("pattern", schema.getPattern());
            }
            if (schema.getMinLength() != null) {
                rule.put("minLength", schema.getMinLength());
            }
            if (schema.getMaxLength() != null) {
                rule.put("maxLength", schema.getMaxLength());
            }
            if (schema.getFormat() != null) {
                rule.put("format", schema.getFormat());
            }
        }
        attachEnum(rule, schema);
        return rule;
    }

    private Map<String, Object> buildRuleFromSchema(String name, Schema<?> schema) {
        if (schema == null) {
            return buildRule("string", name + "-sample");
        }
        String type = schema.getType();
        boolean objectLike = "object".equalsIgnoreCase(type) || schema.getProperties() != null;
        if (schema.get$ref() != null) {
            return buildRule("ref", schema.get$ref());
        }
        if ("integer".equalsIgnoreCase(type) || "long".equalsIgnoreCase(type)) {
            Number sample = schema.getExample() instanceof Number ? (Number) schema.getExample() : 1;
            Map<String, Object> rule = buildNumberRule("integer", sample, schema.getMinimum(), schema.getMaximum());
            attachEnum(rule, schema);
            return rule;
        }
        if ("number".equalsIgnoreCase(type)) {
            Number sample = schema.getExample() instanceof Number ? (Number) schema.getExample() : 1.0;
            Map<String, Object> rule = buildNumberRule("number", sample, schema.getMinimum(), schema.getMaximum());
            attachEnum(rule, schema);
            if (schema.getMultipleOf() != null) {
                rule.put("multipleOf", schema.getMultipleOf());
            }
            return rule;
        }
        if ("boolean".equalsIgnoreCase(type)) {
            Boolean sample = schema.getExample() instanceof Boolean ? (Boolean) schema.getExample() : Boolean.TRUE;
            Map<String, Object> rule = buildBooleanRule(sample);
            attachEnum(rule, schema);
            return rule;
        }
        if ("array".equalsIgnoreCase(type)) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", "array");
            rule.put("item", buildRuleFromSchema(name + "Item", schema.getItems()));
            if (schema.getMinItems() != null) {
                rule.put("minItems", schema.getMinItems());
            }
            if (schema.getMaxItems() != null) {
                rule.put("maxItems", schema.getMaxItems());
            }
            attachEnum(rule, schema);
            return rule;
        }
        if (objectLike) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", "object");
            Map<String, Schema> properties = schema.getProperties();
            if (properties != null && !properties.isEmpty()) {
                Map<String, Object> propertyRules = new LinkedHashMap<>();
                for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                    propertyRules.put(entry.getKey(), buildRuleFromSchema(entry.getKey(), entry.getValue()));
                }
                rule.put("properties", propertyRules);
            }
            if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                rule.put("requiredProperties", new ArrayList<>(schema.getRequired()));
            }
            attachEnum(rule, schema);
            return rule;
        }
        return buildStringRule(name, schema);
    }

    private Map<String, Object> createRuleWithSample(String name, Schema<?> schema) {
        Map<String, Object> rule = buildRuleFromSchema(name, schema);
        if (!rule.containsKey("sample")) {
            rule.put("sample", buildParameterSample(name, schema));
        }
        return rule;
    }

    private Object buildParameterSample(String name, Schema<?> schema) {
        if (schema == null) {
            return name + "-sample";
        }
        if (schema.getExample() != null) {
            return schema.getExample();
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return schema.getEnum().get(0);
        }
        String type = schema.getType();
        if ("array".equalsIgnoreCase(type) || "object".equalsIgnoreCase(type) || schema.getProperties() != null) {
            Object sample = buildBodySample(schema, true);
            if (sample instanceof Map && ((Map<?, ?>) sample).isEmpty()) {
                return name + "-sample";
            }
            return sample;
        }
        if ("boolean".equalsIgnoreCase(type)) {
            return Boolean.TRUE;
        }
        if ("integer".equalsIgnoreCase(type) || "long".equalsIgnoreCase(type)) {
            return 1;
        }
        if ("number".equalsIgnoreCase(type)) {
            return 1.0;
        }
        return name + "-sample";
    }

    private Object buildBodySample(Schema<?> schema, boolean includeOptional) {
        if (schema == null) {
            return Collections.emptyMap();
        }
        if (schema.getExample() != null) {
            return schema.getExample();
        }
        if (schema.get$ref() != null) {
            return Collections.singletonMap("$ref", schema.get$ref());
        }
        String type = schema.getType();
        if ("object".equalsIgnoreCase(type) || schema.getProperties() != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Schema> properties = schema.getProperties();
            Set<String> requiredFields = schema.getRequired() != null ? Set.copyOf(schema.getRequired()) : Collections.emptySet();
            if (properties != null) {
                for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    boolean required = requiredFields.contains(key);
                    if (required || includeOptional) {
                        result.put(key, buildBodySample(entry.getValue(), includeOptional));
                    }
                }
            }
            return result;
        }
        if ("array".equalsIgnoreCase(type)) {
            Schema<?> itemSchema = schema.getItems();
            Object sample = buildBodySample(itemSchema, includeOptional);
            return Collections.singletonList(sample);
        }
        if ("integer".equalsIgnoreCase(type) || "long".equalsIgnoreCase(type)) {
            return schema.getExample() != null ? schema.getExample() : 1;
        }
        if ("number".equalsIgnoreCase(type)) {
            return schema.getExample() != null ? schema.getExample() : 1.0;
        }
        if ("boolean".equalsIgnoreCase(type)) {
            return schema.getExample() != null ? schema.getExample() : Boolean.TRUE;
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return schema.getEnum().get(0);
        }
        return schema.getExample() != null ? schema.getExample() : "sample";
    }

    private void appendBody(TemplateVariantResult result, Object bodySample, boolean required) {
        if (bodySample == null) {
            return;
        }
        String template = result.getCurlTemplate();
        if (!template.contains(BODY_SEGMENT_PREFIX)) {
            template = template + BODY_SEGMENT_TEMPLATE;
        }
        result.setCurlTemplate(template);
        Map<String, Object> rule = buildRule("json", bodySample);
        if (required) {
            rule.put("required", Boolean.TRUE);
        }
        result.getParamRules().put(BODY_VARIABLE_KEY, rule);
    }

    private List<Parameter> resolveParameters(Operation operation) {
        if (operation == null || operation.getParameters() == null) {
            return Collections.emptyList();
        }
        return operation.getParameters();
    }

    private Content resolveContent(RequestBody requestBody) {
        if (requestBody == null) {
            return null;
        }
        return requestBody.getContent();
    }

    private boolean isJsonMediaType(String mediaTypeKey) {
        if (mediaTypeKey == null) {
            return false;
        }
        String lower = mediaTypeKey.toLowerCase();
        return lower.contains("json") || lower.contains("text/javascript");
    }

    private MediaType pickMediaType(Content content) {
        if (content == null) {
            return null;
        }
        if (content.size() == 1) {
            return content.values().iterator().next();
        }
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            if (isJsonMediaType(entry.getKey())) {
                return entry.getValue();
            }
        }
        return content.values().iterator().next();
    }

    private void appendHeaderRules(TemplateVariantResult result, Parameter parameter) {
        String headerName = parameter.getName();
        Map<String, Object> rule = buildRuleForParameter(parameter);
        result.getParamRules().put(HEADER_PREFIX + headerName, rule);
        String placeholder = "${" + HEADER_PREFIX + headerName + "}";
        if (!result.getCurlTemplate().contains(placeholder)) {
            result.setCurlTemplate(result.getCurlTemplate() +
                    String.format(HEADER_SEGMENT_TEMPLATE, headerName, placeholder));
        }
    }

    private void appendQueryPlaceholder(TemplateVariantResult result, String queryName, boolean alreadyHasQuery) {
        String placeholder = "${" + QUERY_PREFIX + queryName + "}";
        if (!result.getCurlTemplate().contains(placeholder)) {
            StringBuilder builder = new StringBuilder(result.getCurlTemplate());
            builder.append(alreadyHasQuery ? '&' : '?')
                    .append(queryName)
                    .append('=')
                    .append(placeholder);
            result.setCurlTemplate(builder.toString());
        }
    }

    private void appendQueryRule(TemplateVariantResult result, Parameter parameter) {
        Map<String, Object> rule = buildRuleForParameter(parameter);
        result.getParamRules().put(QUERY_PREFIX + parameter.getName(), rule);
    }

    private void ensureDefaultHeaders(TemplateVariantResult result) {
        String template = result.getCurlTemplate();
        if (!template.contains(CONTENT_TYPE_HEADER)) {
            template = template + DEFAULT_CONTENT_TYPE_SEGMENT;
        }
        if (!template.contains(ACCEPT_HEADER)) {
            template = template + DEFAULT_ACCEPT_SEGMENT;
        }
        result.setCurlTemplate(template);
    }

    private class MinimalVariantBuilder implements TemplateVariantBuilder {

        @Override
        public TemplateVariantResult build(TemplateContext context) {
            TemplateVariantResult result = buildBaseResult(context);
            List<Parameter> parameters = resolveParameters(context.operation());
            boolean hasQuery = result.getCurlTemplate().contains("?");
            for (Parameter parameter : parameters) {
                if (!Boolean.TRUE.equals(parameter.getRequired())) {
                    continue;
                }
                if ("query".equalsIgnoreCase(parameter.getIn())) {
                    appendQueryRule(result, parameter);
                    appendQueryPlaceholder(result, parameter.getName(), hasQuery);
                    hasQuery = true;
                } else if ("header".equalsIgnoreCase(parameter.getIn())) {
                    appendHeaderRules(result, parameter);
                }
            }
            ensureDefaultHeaders(result);
            appendBodyIfNecessary(context, result, false);
            return result;
        }
    }

    private class FullVariantBuilder implements TemplateVariantBuilder {

        @Override
        public TemplateVariantResult build(TemplateContext context) {
            TemplateVariantResult result = buildBaseResult(context);
            List<Parameter> parameters = resolveParameters(context.operation());
            boolean hasQuery = result.getCurlTemplate().contains("?");
            for (Parameter parameter : parameters) {
                if ("query".equalsIgnoreCase(parameter.getIn())) {
                    appendQueryRule(result, parameter);
                    appendQueryPlaceholder(result, parameter.getName(), hasQuery);
                    hasQuery = true;
                } else if ("header".equalsIgnoreCase(parameter.getIn())) {
                    appendHeaderRules(result, parameter);
                }
            }
            ensureDefaultHeaders(result);
            appendBodyIfNecessary(context, result, true);
            return result;
        }
    }

    private void appendBodyIfNecessary(TemplateContext context, TemplateVariantResult result, boolean includeOptional) {
        RequestBody requestBody = context.operation() != null ? context.operation().getRequestBody() : null;
        if (requestBody == null) {
            return;
        }
        Content content = resolveContent(requestBody);
        MediaType mediaType = pickMediaType(content);
        if (mediaType == null) {
            return;
        }
        Schema<?> schema = mediaType.getSchema();
        Object bodySample = buildBodySample(schema, includeOptional);
        boolean required = Boolean.TRUE.equals(requestBody.getRequired());
        if (bodySample == null || (bodySample instanceof Map && ((Map<?, ?>) bodySample).isEmpty())) {
            if (required) {
                appendBody(result, Collections.emptyMap(), true);
            }
            return;
        }
        appendBody(result, bodySample, required);
    }

    private record TemplateContext(GroupDO group, Operation operation, TemplateEnvironment environment) {
    }

    private record TemplateEnvironment(String host, Map<String, String> headers, Map<String, Object> variables) {
    }

    private static class TemplateVariantResult {
        private String curlTemplate;
        private final Map<String, Map<String, Object>> paramRules;

        TemplateVariantResult(String curlTemplate, Map<String, Map<String, Object>> paramRules) {
            this.curlTemplate = curlTemplate;
            this.paramRules = paramRules;
        }

        String getCurlTemplate() {
            return curlTemplate;
        }

        void setCurlTemplate(String curlTemplate) {
            this.curlTemplate = curlTemplate;
        }

        Map<String, Map<String, Object>> getParamRules() {
            return paramRules;
        }
    }

    private interface TemplateVariantBuilder {
        TemplateVariantResult build(TemplateContext context);
    }
}
