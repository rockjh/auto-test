package com.example.autotest.infra.core.id;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BizIdGenerator {

    private final AtomicInteger sequence = new AtomicInteger(0);

    public long nextId() {
        long timestamp = Instant.now().toEpochMilli();
        int seq = sequence.updateAndGet(current -> current >= 4095 ? 0 : current + 1);
        return (timestamp << 12) | seq;
    }
}
