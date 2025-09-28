package com.skyler.autotest.executor.lock;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简易的场景级互斥锁注册表，确保同一场景不会被并发执行。
 */
@Component
public class ScenarioLockRegistry {

    private final Map<Long, ReentrantLock> lockPool = new ConcurrentHashMap<>();

    /**
     * 尝试在给定超时内获取场景锁。
     *
     * @param scenarioId 场景编号
     * @param waitMillis 等待毫秒数
     * @return 是否成功获取
     * @throws InterruptedException 中断异常
     */
    public boolean tryLock(Long scenarioId, long waitMillis) throws InterruptedException {
        Objects.requireNonNull(scenarioId, "scenarioId");
        ReentrantLock lock = lockPool.computeIfAbsent(scenarioId, id -> new ReentrantLock());
        return lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 释放场景锁。
     *
     * @param scenarioId 场景编号
     */
    public void unlock(Long scenarioId) {
        if (scenarioId == null) {
            return;
        }
        ReentrantLock lock = lockPool.get(scenarioId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
