package com.example.autotest.template.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.autotest.infra.core.domain.BaseDO;
import lombok.Data;

@Data
@TableName("autotest_curl_variant")
public class CurlVariantDO extends BaseDO {
    private Long projectId;
    private Long groupId;
    private String variantType;
    private String versionNo;
    private String curlTemplate;
    private String paramRules;
    private String generatorConfig;
    private String ruleVersion;
    private Long editorId;
    private String editorName;
    private Boolean needReview;
    private String remark;
}
