package com.skyler.autotest.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.template.api.TemplateService;
import com.skyler.autotest.template.api.dto.CurlVariantResponse;
import com.skyler.autotest.template.api.dto.TemplatePreviewRequest;
import com.skyler.autotest.template.api.enums.TemplateChangeType;
import com.skyler.autotest.template.api.enums.TemplateVariantType;
import com.skyler.autotest.template.converter.TemplateConverter;
import com.skyler.autotest.template.dal.dataobject.CurlVariantDO;
import com.skyler.autotest.template.dal.mapper.CurlVariantMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.skyler.autotest.template.api.constant.TemplateConstants.BODY_PLACEHOLDER;
import static com.skyler.autotest.template.api.constant.TemplateConstants.BODY_VARIABLE_KEY;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_HOST_FALLBACK;
import static com.skyler.autotest.template.api.constant.TemplateConstants.DEFAULT_SAMPLE_RANDOM_BOUND;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HEADER_PREFIX;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HOST_PLACEHOLDER;
import static com.skyler.autotest.template.api.constant.TemplateConstants.HOST_VARIABLE_KEY;
import static com.skyler.autotest.template.api.constant.TemplateConstants.PATH_PREFIX;

/**
 * 模板模块对外提供的应用服务，实现模板查询、重生成与预览能力。
 */
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final CurlVariantMapper curlVariantMapper;
    private final TemplateGenerationService templateGenerationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CurlVariantResponse> listVariantsByGroup(Long groupId) {
        List<CurlVariantDO> list = curlVariantMapper.selectList(new LambdaQueryWrapper<CurlVariantDO>()
                .eq(CurlVariantDO::getGroupId, groupId)
                .orderByAsc(CurlVariantDO::getVariantType));
        return list.stream().map(TemplateConverter::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CurlVariantResponse getVariant(Long variantId) {
        if (variantId == null) {
            return null;
        }
        CurlVariantDO variant = curlVariantMapper.selectById(variantId);
        if (variant == null) {
            return null;
        }
        return TemplateConverter.toResponse(variant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CurlVariantResponse regenerateMinimalVariant(Long groupId) {
        CurlVariantDO minimal = templateGenerationService.generateMinimalVariant(groupId, false, TemplateChangeType.MANUAL);
        return TemplateConverter.toResponse(minimal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CurlVariantResponse regenerateVariant(Long groupId, TemplateVariantType variantType) {
        CurlVariantDO variant = templateGenerationService.generateVariant(groupId, variantType, false, TemplateChangeType.MANUAL);
        return TemplateConverter.toResponse(variant);
    }

    /**
     * 生成模板的预览命令，应用参数规则与外部变量。
     */
    @Override
    @Transactional(readOnly = true)
    public String preview(Long variantId, TemplatePreviewRequest request) {
        CurlVariantDO variant = curlVariantMapper.selectById(variantId);
        if (variant == null) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_VARIANT_NOT_FOUND);
        }
        Map<String, Object> variables = new HashMap<>();
        mergeRuleSamples(variant.getParamRules(), variables);
        if (request != null && request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        return applyVariables(variant.getCurlTemplate(), variables);
    }

    /**
     * 将模板规则中的示例值合并到变量集合，供预览渲染使用。
     *
     * @param paramRulesJson 参数规则 JSON 字符串
     * @param variables      变量结果集合
     */
    private void mergeRuleSamples(String paramRulesJson, Map<String, Object> variables) {
        if (StringUtils.isBlank(paramRulesJson)) {
            return;
        }
        try {
            Map<String, Map<String, Object>> rules = objectMapper.readValue(paramRulesJson, new TypeReference<>() {
            });
            rules.forEach((key, value) -> {
                Object sample = value.get("sample");
                if (sample == null) {
                    sample = ThreadLocalRandom.current().nextInt(DEFAULT_SAMPLE_RANDOM_BOUND);
                }
                variables.put(key, sample);
            });
        } catch (IOException ex) {
            throw new BizException(AutoTestErrorCode.TEMPLATE_PARAM_RULE_INVALID,
                    "模板参数规则解析失败", ex);
        }
    }

    /**
     * 将变量键值对替换到模板字符串中，兼容路径、主机与请求体占位符。
     *
     * @param template 原始模板
     * @param variables 变量键值对
     * @return 渲染后的模板
     */
    private String applyVariables(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith(PATH_PREFIX)) {
                String placeholder = "{" + key.substring(PATH_PREFIX.length()) + "}";
                result = result.replace(placeholder, String.valueOf(value));
            } else if (key.startsWith(HEADER_PREFIX)) {
                // headers handled separately in the future
            } else if (HOST_VARIABLE_KEY.equals(key)) {
                result = result.replace(HOST_PLACEHOLDER, String.valueOf(value));
            } else if (BODY_VARIABLE_KEY.equals(key)) {
                result = result.replace(BODY_PLACEHOLDER, String.valueOf(value));
            }
        }
        if (result.contains(HOST_PLACEHOLDER)) {
            result = result.replace(HOST_PLACEHOLDER, DEFAULT_HOST_FALLBACK);
        }
        return result;
    }
}
