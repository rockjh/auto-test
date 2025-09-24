package com.example.autotest.template.converter;

import com.example.autotest.template.api.dto.CurlVariantResponse;
import com.example.autotest.template.dal.dataobject.CurlVariantDO;

public class TemplateConverter {

    private TemplateConverter() {
    }

    public static CurlVariantResponse toResponse(CurlVariantDO variant) {
        if (variant == null) {
            return null;
        }
        CurlVariantResponse response = new CurlVariantResponse();
        response.setId(variant.getId());
        response.setProjectId(variant.getProjectId());
        response.setGroupId(variant.getGroupId());
        response.setVariantType(variant.getVariantType());
        response.setCurlTemplate(variant.getCurlTemplate());
        response.setParamRules(variant.getParamRules());
        response.setNeedReview(variant.getNeedReview());
        return response;
    }
}
