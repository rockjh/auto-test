package com.skyler.autotest.template.converter;

import com.skyler.autotest.template.api.dto.CurlVariantResponse;
import com.skyler.autotest.template.dal.dataobject.CurlVariantDO;

public class TemplateConverter {

    private TemplateConverter() {
    }

    /**
     * 将数据对象转换为对外响应对象，便于 REST 层序列化。
     *
     * @param variant 数据对象
     * @return 响应对象
     */
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
