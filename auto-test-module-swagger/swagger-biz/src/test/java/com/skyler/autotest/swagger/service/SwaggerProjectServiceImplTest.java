package com.skyler.autotest.swagger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.skyler.autotest.infra.core.exception.BizException;
import com.skyler.autotest.infra.core.id.BizIdGenerator;
import com.skyler.autotest.infra.core.util.AutoTestJsonUtils;
import com.skyler.autotest.infra.event.SwaggerSyncEvent;
import com.skyler.autotest.swagger.api.dto.ProjectSyncRequest;
import com.skyler.autotest.swagger.dal.dataobject.CollectionDO;
import com.skyler.autotest.swagger.dal.dataobject.GroupDO;
import com.skyler.autotest.swagger.dal.dataobject.ProjectDO;
import com.skyler.autotest.swagger.dal.dataobject.SwaggerSyncDO;
import com.skyler.autotest.swagger.dal.mapper.CollectionMapper;
import com.skyler.autotest.swagger.dal.mapper.GroupMapper;
import com.skyler.autotest.swagger.dal.mapper.ProjectMapper;
import com.skyler.autotest.swagger.dal.mapper.SwaggerSyncMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwaggerProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private CollectionMapper collectionMapper;
    @Mock
    private GroupMapper groupMapper;
    @Mock
    private SwaggerSyncMapper swaggerSyncMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RestTemplate restTemplate;

    private SwaggerProjectServiceImpl service;
    private ProjectDO project;

    @BeforeEach
    void setUp() {
        BizIdGenerator bizIdGenerator = new BizIdGenerator();
        AutoTestJsonUtils jsonUtils = new AutoTestJsonUtils(new ObjectMapper());
        service = new SwaggerProjectServiceImpl(projectMapper, collectionMapper, groupMapper, swaggerSyncMapper,
                bizIdGenerator, jsonUtils, new ObjectMapper(), eventPublisher, restTemplate);

        project = new ProjectDO();
        project.setId(1001L);
        project.setTenantId(2001L);
        project.setSwaggerSource("http://swagger-host/api.json");
        project.setSwaggerType(1);
        project.setStatus(1);
    }

    @Test
    void syncProject_shouldGenerateGroupsAndEmitEvent() throws Exception {
        when(projectMapper.selectById(1001L)).thenReturn(project);

        OpenAPI openAPI = buildOpenApi();
        String json = new ObjectMapper().writeValueAsString(openAPI);
        when(restTemplate.exchange(eq(project.getSwaggerSource()), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        AtomicReference<GroupDO> insertedGroup = new AtomicReference<>();
        doAnswer(invocation -> {
            GroupDO value = invocation.getArgument(0);
            insertedGroup.set(value);
            return null;
        }).when(groupMapper).insert(any(GroupDO.class));

        doAnswer(invocation -> null).when(swaggerSyncMapper).insert(any(SwaggerSyncDO.class));
        when(swaggerSyncMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.syncProject(1001L, new ProjectSyncRequest().setAllowRemoteFetch(true));

        GroupDO group = insertedGroup.get();
        assertNotNull(group);
        assertEquals("GET", group.getMethod());
        assertEquals("/orders/{id}", group.getPath());

        ArgumentCaptor<SwaggerSyncEvent> eventCaptor = ArgumentCaptor.forClass(SwaggerSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SwaggerSyncEvent event = eventCaptor.getValue();
        assertEquals(project.getTenantId(), event.getTenantId());
        assertNotNull(event.getAddedGroupIds());
        assertEquals(1, event.getAddedGroupIds().size());
    }

    @Test
    void syncProject_shouldThrowWhenProjectMissing() {
        when(projectMapper.selectById(1001L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.syncProject(1001L, null));
    }

    private OpenAPI buildOpenApi() {
        OpenAPI openAPI = new OpenAPI();
        Paths paths = new Paths();
        PathItem pathItem = new PathItem();
        Operation getOperation = new Operation();
        getOperation.setSummary("查询订单");
        getOperation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                .name("id")
                .in("path")
                .required(true)
                .schema(new StringSchema().example("10001")));
        pathItem.setGet(getOperation);
        paths.addPathItem("/orders/{id}", pathItem);
        openAPI.setPaths(paths);
        return openAPI;
    }
}
