package com.skyler.autotest.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.template.api.enums.TemplateChangeType;
import com.skyler.autotest.template.api.enums.TemplateVariantType;
import com.skyler.autotest.template.dal.dataobject.CurlVariantDO;
import com.skyler.autotest.template.dal.mapper.CurlVariantMapper;
import com.skyler.autotest.swagger.dal.dataobject.GroupDO;
import com.skyler.autotest.swagger.dal.dataobject.ProjectEnvironmentDO;
import com.skyler.autotest.swagger.dal.mapper.GroupMapper;
import com.skyler.autotest.swagger.dal.mapper.ProjectEnvironmentMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateGenerationServiceTest {

    @Mock
    private CurlVariantMapper curlVariantMapper;
    @Mock
    private GroupMapper groupMapper;
    @Mock
    private ProjectEnvironmentMapper projectEnvironmentMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TemplateGenerationService service;

    @BeforeEach
    void setUp() {
        BizIdGenerator bizIdGenerator = new BizIdGenerator();
        service = new TemplateGenerationService(curlVariantMapper, groupMapper, projectEnvironmentMapper,
                bizIdGenerator, objectMapper, eventPublisher);
        service.registerBuilders();

        doAnswer(invocation -> null).when(curlVariantMapper).insert(any(CurlVariantDO.class));
    }

    @Test
    void generateVariant_shouldIncludeEnvironmentHeadersAndSamples() throws Exception {
        long groupId = 10101L;
        GroupDO group = buildGroup(groupId);
        ProjectEnvironmentDO environment = buildEnvironment(group);

        when(groupMapper.selectById(groupId)).thenReturn(group);
        when(curlVariantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(projectEnvironmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(environment));

        CurlVariantDO minimalVariant = service.generateVariant(groupId, TemplateVariantType.MINIMAL, false, TemplateChangeType.SYNC_UPDATE);
        CurlVariantDO fullVariant = service.generateVariant(groupId, TemplateVariantType.FULL, true, TemplateChangeType.SYNC_UPDATE);

        assertNotNull(minimalVariant);
        assertNotNull(fullVariant);

        Map<String, Map<String, Object>> minimalRules = objectMapper.readValue(minimalVariant.getParamRules(), new TypeReference<>() {
        });
        Map<String, Map<String, Object>> fullRules = objectMapper.readValue(fullVariant.getParamRules(), new TypeReference<>() {
        });

        assertEquals("https://qa.example.com", minimalRules.get("host").get("sample"));
        assertEquals("Bearer qa-token", minimalRules.get("header.Authorization").get("sample"));
        assertEquals("PROCESSING", minimalRules.get("query.status").get("sample"));
        assertTrue(minimalRules.containsKey("body"));
        assertTrue(minimalRules.containsKey("path.orderId"));

        assertTrue(minimalVariant.getCurlTemplate().contains("${header.Authorization}"));
        assertTrue(minimalVariant.getCurlTemplate().contains("?status=${query.status}"));
        assertTrue(minimalVariant.getCurlTemplate().contains(" -d '${body}'"));

        Map<String, Object> fullBodyRule = fullRules.get("body");
        assertNotNull(fullBodyRule);
        Map<?, ?> bodySample = (Map<?, ?>) fullBodyRule.get("sample");
        assertEquals("demo-name", bodySample.get("name"));

        assertTrue(fullVariant.getCurlTemplate().contains("${header.Authorization}"));
        assertTrue(fullVariant.getCurlTemplate().contains(" -d '${body}'"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(eventCaptor.capture());
    }

    private GroupDO buildGroup(long groupId) throws Exception {
        GroupDO group = new GroupDO();
        group.setId(groupId);
        group.setProjectId(20001L);
        group.setTenantId(30001L);
        group.setMethod("POST");
        group.setPath("/orders/{orderId}");
        group.setHash("hash-001");

        Operation operation = new Operation();
        Parameter queryParam = new Parameter()
                .name("status")
                .in("query")
                .required(true)
                .schema(new StringSchema().example("PROCESSING"));
        operation.setParameters(List.of(queryParam));

        ObjectSchema bodySchema = new ObjectSchema()
                .addProperties("name", new StringSchema().example("demo-name"))
                .required(List.of("name"));
        MediaType jsonMediaType = new MediaType().schema(bodySchema);
        Content content = new Content().addMediaType("application/json", jsonMediaType);
        RequestBody requestBody = new RequestBody().content(content).required(true);
        operation.setRequestBody(requestBody);

        group.setRequestSchema(objectMapper.writeValueAsString(operation));
        return group;
    }

    private ProjectEnvironmentDO buildEnvironment(GroupDO group) throws Exception {
        ProjectEnvironmentDO environment = new ProjectEnvironmentDO();
        environment.setId(501L);
        environment.setTenantId(group.getTenantId());
        environment.setProjectId(group.getProjectId());
        environment.setHost("https://qa.example.com");
        environment.setHeaders(objectMapper.writeValueAsString(Map.of("Authorization", "Bearer qa-token")));
        environment.setIsDefault(true);
        environment.setStatus(1);
        environment.setDeleted(Boolean.FALSE);
        return environment;
    }
}
