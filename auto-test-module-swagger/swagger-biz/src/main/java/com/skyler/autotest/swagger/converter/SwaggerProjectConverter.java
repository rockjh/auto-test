package com.skyler.autotest.swagger.converter;

import com.skyler.autotest.swagger.api.dto.ProjectResponse;
import com.skyler.autotest.swagger.dal.dataobject.ProjectDO;

public class SwaggerProjectConverter {

    private SwaggerProjectConverter() {
    }

    public static ProjectResponse toResponse(ProjectDO projectDO) {
        if (projectDO == null) {
            return null;
        }
        ProjectResponse response = new ProjectResponse();
        response.setId(projectDO.getId());
        response.setName(projectDO.getName());
        response.setSwaggerSource(projectDO.getSwaggerSource());
        response.setSwaggerType(projectDO.getSwaggerType());
        response.setSwaggerVersion(projectDO.getSwaggerVersion());
        response.setSwaggerHash(projectDO.getSwaggerHash());
        response.setSyncStatus(projectDO.getSyncStatus());
        response.setSyncTime(projectDO.getSyncTime());
        return response;
    }
}
