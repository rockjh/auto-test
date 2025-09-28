package com.skyler.autotest.infra.core.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段自动填充处理器，保证审计字段一致。
 */
@Component
@ConditionalOnMissingBean(MetaObjectHandler.class)
public class AuditingMetaObjectHandler implements MetaObjectHandler {

    /** 默认操作人标记，统一落库便于追踪系统动作。 */
    private static final String SYSTEM_OPERATOR = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "creator", String.class, SYSTEM_OPERATOR);
        strictInsertFill(metaObject, "updater", String.class, SYSTEM_OPERATOR);
        strictInsertFill(metaObject, "deleted", Boolean.class, Boolean.FALSE);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updater", String.class, SYSTEM_OPERATOR);
    }
}
