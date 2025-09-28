package com.skyler.autotest.template.api;

import com.skyler.autotest.template.api.dto.CurlVariantResponse;
import com.skyler.autotest.template.api.dto.TemplatePreviewRequest;
import com.skyler.autotest.template.api.enums.TemplateVariantType;

import java.util.List;

/**
 * 模板模块对外暴露的应用服务，供 REST 层或其他模块查询与维护模板数据。
 */
public interface TemplateService {

    /**
     * 查询指定接口分组下的全部模板。
     *
     * @param groupId 接口分组 ID
     * @return 模板集合
     */
    List<CurlVariantResponse> listVariantsByGroup(Long groupId);

    /**
     * 根据模板主键查询模板详情。
     *
     * @param variantId 模板 ID
     * @return 模板详情，未查询到返回 {@code null}
     */
    CurlVariantResponse getVariant(Long variantId);

    /**
     * 重新生成最小模板。
     *
     * @param groupId 接口分组 ID
     * @return 最新的最小模板信息
     */
    CurlVariantResponse regenerateMinimalVariant(Long groupId);

    /**
     * 按类型重新生成模板。
     *
     * @param groupId     接口分组 ID
     * @param variantType 模板类型
     * @return 最新的模板信息
     */
    CurlVariantResponse regenerateVariant(Long groupId, TemplateVariantType variantType);

    /**
     * 预览模板渲染后的 curl 命令。
     *
     * @param variantId 模板 ID
     * @param request   变量覆盖信息，可为空
     * @return 渲染后的 curl 命令
     */
    String preview(Long variantId, TemplatePreviewRequest request);
}
