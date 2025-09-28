package com.skyler.autotest.infra.core.context;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;

/**
 * 租户上下文工具，封装从安全框架获取当前租户的逻辑，提供默认租户兜底。
 */
public final class TenantContextHolder {

    /** 默认租户编号，兼容本地或未登录场景。 */
    private static final long DEFAULT_TENANT_ID = 1L;

    private TenantContextHolder() {
    }

    /**
     * 返回当前登录用户的租户编号，若无法识别则回退到默认租户。
     *
     * @return 租户编号
     */
    public static long getRequiredTenantId() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && loginUser.getTenantId() != null) {
            return loginUser.getTenantId();
        }
        return DEFAULT_TENANT_ID;
    }
}

