package com.skyler.autotest.template.api.constant;

/**
 * 模板模块使用的通用常量，统一管理变量占位与默认配置，避免硬编码散落在各业务类中。
 */
public final class TemplateConstants {

    private TemplateConstants() {
    }

    public static final String HOST_VARIABLE_KEY = "host";
    public static final String HOST_PLACEHOLDER = "${host}";

    public static final String HEADER_PREFIX = "header.";
    public static final String PATH_PREFIX = "path.";
    public static final String QUERY_PREFIX = "query.";
    public static final String BODY_VARIABLE_KEY = "body";
    public static final String BODY_PLACEHOLDER = "${body}";

    public static final String DEFAULT_SAMPLE_SUFFIX = "-sample";
    public static final String DEFAULT_HTTP_METHOD = "GET";
    public static final String DEFAULT_HOST_FALLBACK = "http://localhost";

    public static final String HEADER_SEGMENT_TEMPLATE = " -H '%s: %s'";
    public static final String DEFAULT_CONTENT_TYPE_SEGMENT = " -H 'Content-Type: application/json'";
    public static final String DEFAULT_ACCEPT_SEGMENT = " -H 'Accept: application/json'";

    public static final String BODY_SEGMENT_PREFIX = " -d '";
    public static final String BODY_SEGMENT_TEMPLATE = " -d '${body}'";

    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String ACCEPT_HEADER = "Accept";

    /** 随机样本生成的上限（exclusive），用于缺省示例值。 */
    public static final int DEFAULT_SAMPLE_RANDOM_BOUND = 1000;
}
