package com.skyler.autotest.infra.core.security;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.skyler.autotest.infra.core.error.AutoTestErrorCode;
import com.skyler.autotest.infra.core.exception.BizException;
import lombok.experimental.UtilityClass;

/**
 * 安全上下文工具类，封装登录用户与租户信息的获取逻辑，避免业务层硬编码。
 */
@UtilityClass
public class SecurityContextUtils {

    /**
     * 获取当前登录用户。
     *
     * @return 登录用户，未登录时返回 {@code null}
     */
    public static LoginUser getLoginUser() {
        return SecurityFrameworkUtils.getLoginUser();
    }

    /**
     * 获取当前请求关联的租户编号。
     * 优先从登录用户的访问租户获取，兜底读取 {@link TenantContextHolder}。
     *
     * @return 租户编号，未登录或上下文缺失时返回 {@code null}
     */
    public static Long getTenantId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser != null) {
            if (loginUser.getVisitTenantId() != null) {
                return loginUser.getVisitTenantId();
            }
            return loginUser.getTenantId();
        }
        return TenantContextHolder.getTenantId();
    }

    /**
     * 获取当前租户编号（必填），若缺失则抛出业务异常。
     *
     * @return 租户编号
     * @throws BizException 未登录或上下文缺失租户信息时抛出
     */
    public static Long getRequiredTenantId() {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new BizException(AutoTestErrorCode.UNAUTHORIZED);
        }
        return tenantId;
    }
}
