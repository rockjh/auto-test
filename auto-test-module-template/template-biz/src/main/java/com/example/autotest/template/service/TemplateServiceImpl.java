package com.example.autotest.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.autotest.infra.core.exception.BizException;
import com.example.autotest.template.api.TemplateService;
import com.example.autotest.template.api.dto.CurlVariantResponse;
import com.example.autotest.template.api.dto.TemplatePreviewRequest;
import com.example.autotest.template.converter.TemplateConverter;
import com.example.autotest.template.dal.dataobject.CurlVariantDO;
import com.example.autotest.template.dal.mapper.CurlVariantMapper;
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
import java.util.Random;

/**
 * 模板模块对外提供的应用服务，实现模板查询、重生成与预览能力。
 */
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final CurlVariantMapper curlVariantMapper;
    private final TemplateGenerationService templateGenerationService;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public List<CurlVariantResponse> listVariantsByGroup(Long groupId) {
        List<CurlVariantDO> list = curlVariantMapper.selectList(new LambdaQueryWrapper<CurlVariantDO>()
                .eq(CurlVariantDO::getGroupId, groupId)
                .orderByAsc(CurlVariantDO::getVariantType));
        return list.stream().map(TemplateConverter::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CurlVariantResponse regenerateMinimalVariant(Long groupId) {
        CurlVariantDO minimal = templateGenerationService.generateMinimalVariant(groupId, false, "MANUAL");
        return TemplateConverter.toResponse(minimal);
    }

    /**
     * 生成模板的预览命令，应用参数规则与外部变量。
     */
    @Override
    public String preview(Long variantId, TemplatePreviewRequest request) {
        CurlVariantDO variant = curlVariantMapper.selectById(variantId);
        if (variant == null) {
            throw new BizException(404, "variant not found");
        }
        Map<String, Object> variables = new HashMap<>();
        if (StringUtils.isNotBlank(variant.getParamRules())) {
            try {
                Map<String, Map<String, Object>> rules = objectMapper.readValue(variant.getParamRules(), new TypeReference<>() {});
                rules.forEach((key, value) -> variables.put(key, value.getOrDefault("sample", random.nextInt(1000))));
            } catch (IOException e) {
                throw new BizException(500, "invalid param rules");
            }
        }
        if (request != null && request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        return applyVariables(variant.getCurlTemplate(), variables);
    }

    private String applyVariables(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("path.")) {
                String placeholder = "{" + key.substring(5) + "}";
                result = result.replace(placeholder, String.valueOf(value));
            } else if (key.startsWith("header.")) {
                // headers handled separately in the future
            } else if ("host".equals(key)) {
                result = result.replace("${host}", String.valueOf(value));
            }
        }
        if (result.contains("${host}")) {
            result = result.replace("${host}", "http://localhost");
        }
        return result;
    }
}
