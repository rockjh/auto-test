package com.example.autotest.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.autotest.infra.core.exception.BizException;
import com.example.autotest.infra.core.id.BizIdGenerator;
import com.example.autotest.infra.event.TemplateUpdateEvent;
import com.example.autotest.template.dal.dataobject.CurlVariantDO;
import com.example.autotest.template.dal.mapper.CurlVariantMapper;
import com.example.autotest.swagger.dal.dataobject.GroupDO;
import com.example.autotest.swagger.dal.mapper.GroupMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 根据最新 Swagger 元数据生成或更新 curl 模板，并发布模板变更事件。
 */
@Service
@RequiredArgsConstructor
public class TemplateGenerationService {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

    private final CurlVariantMapper curlVariantMapper;
    private final GroupMapper groupMapper;
    private final BizIdGenerator bizIdGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 生成或更新指定接口的最小必填模板。
     *
     * @param groupId        接口分组 ID
     * @param markNeedReview 是否需要复核
     * @param changeType     变更类型
     * @return 持久化后的模板 DO
     */
    @Transactional(rollbackFor = Exception.class)
    public CurlVariantDO generateMinimalVariant(Long groupId, boolean markNeedReview, String changeType) {
        GroupDO group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BizException(404, "group not found");
        }
        CurlVariantDO variant = curlVariantMapper.selectOne(new LambdaQueryWrapper<CurlVariantDO>()
                .eq(CurlVariantDO::getGroupId, groupId)
                .eq(CurlVariantDO::getVariantType, "minimal"));
        boolean create = variant == null;
        if (create) {
            variant = new CurlVariantDO();
            variant.setId(bizIdGenerator.nextId());
            variant.setTenantId(group.getTenantId());
            variant.setProjectId(group.getProjectId());
            variant.setGroupId(groupId);
            variant.setVariantType("minimal");
        }
        variant.setTenantId(group.getTenantId());
        variant.setProjectId(group.getProjectId());
        variant.setGroupId(groupId);
        variant.setCurlTemplate(buildCurlTemplate(group));
        variant.setParamRules(buildParamRules(group));
        variant.setVersionNo(group.getHash());
        variant.setRuleVersion(group.getHash());
        variant.setNeedReview(markNeedReview);
        variant.setDeleted(Boolean.FALSE);

        if (create) {
            curlVariantMapper.insert(variant);
        } else {
            curlVariantMapper.updateById(variant);
        }

        publishTemplateEvent(variant, changeType, markNeedReview);
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
            publishTemplateEvent(variant, "DELETE", true);
            LambdaUpdateWrapper<CurlVariantDO> wrapper = new LambdaUpdateWrapper<CurlVariantDO>()
                    .eq(CurlVariantDO::getId, variant.getId())
                    .set(CurlVariantDO::getNeedReview, true)
                    .set(CurlVariantDO::getDeleted, true);
            curlVariantMapper.update(null, wrapper);
        }
    }

    private void publishTemplateEvent(CurlVariantDO variant, String changeType, boolean needReview) {
        TemplateUpdateEvent event = new TemplateUpdateEvent(
                variant.getTenantId(),
                variant.getProjectId(),
                variant.getGroupId(),
                variant.getId(),
                variant.getVariantType(),
                changeType,
                variant.getCurlTemplate(),
                variant.getParamRules(),
                needReview
        );
        eventPublisher.publishEvent(event);
    }

    private String buildCurlTemplate(GroupDO group) {
        StringBuilder builder = new StringBuilder();
        builder.append("curl -X ").append(StringUtils.defaultIfBlank(group.getMethod(), "GET")).append(' ');
        builder.append("\"${host}");
        builder.append(group.getPath()).append("\"");
        builder.append(" -H 'Content-Type: application/json'");
        builder.append(" -H 'Accept: application/json'");
        builder.append(" -d '");
        builder.append("{}");
        builder.append("'");
        return builder.toString();
    }

    private String buildParamRules(GroupDO group) {
        Map<String, Map<String, Object>> rules = new LinkedHashMap<>();
        Matcher matcher = PATH_PARAM_PATTERN.matcher(group.getPath());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (matcher.find()) {
            String param = matcher.group(1);
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", "string");
            rule.put("sample", param + random.nextInt(10, 99));
            rules.put("path." + param, rule);
        }
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "failed to serialize rules");
        }
    }
}
