package com.skyler.autotest.infra.core.id;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于时间戳 + 自增序列的轻量级 ID 生成器，满足单机场景需求。
 */
@Component
public class BizIdGenerator {

    /** 序列号最大值（12 位），超过后自动回绕。 */
    private static final int MAX_SEQUENCE = 4095;

    /** 单机自增序列，保障同毫秒多次请求时仍具唯一性。 */
    private final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * 生成下一个业务 ID。
     *
     * @return 全局唯一 ID
     */
    public long nextId() {
        long timestamp = Instant.now().toEpochMilli();
        int seq = sequence.updateAndGet(current -> current >= MAX_SEQUENCE ? 0 : current + 1);
        return (timestamp << 12) | seq;
    }
}
