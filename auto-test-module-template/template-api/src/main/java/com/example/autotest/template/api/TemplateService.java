package com.example.autotest.template.api;

import com.example.autotest.template.api.dto.CurlVariantResponse;
import com.example.autotest.template.api.dto.TemplatePreviewRequest;

import java.util.List;

public interface TemplateService {

    List<CurlVariantResponse> listVariantsByGroup(Long groupId);

    CurlVariantResponse regenerateMinimalVariant(Long groupId);

    String preview(Long variantId, TemplatePreviewRequest request);
}
