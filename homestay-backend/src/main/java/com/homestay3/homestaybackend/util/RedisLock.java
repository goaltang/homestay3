package com.homestay3.homestaybackend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Redis 分布式锁工具类
 * 使用 Lua 脚本实现原子性解锁，防止误删其他线程/节点的锁
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";

    /**
     * Lua 脚本：原子性解锁
     * 只有当锁的值等于传入的 requestId 时才删除锁，防止误删
     */
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * 尝试获取分布式锁
     *
     * @param key       锁的业务键（会自动添加 lock: 前缀）
     * @param requestId 请求标识，用于解锁时校验身份
     * @param timeout   锁的过期时间，防止死锁
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, String requestId, Duration timeout) {
        String lockKey = LOCK_PREFIX + key;
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, timeout);
            boolean acquired = Boolean.TRUE.equals(success);
            if (acquired) {
                log.debug("成功获取分布式锁: key={}, requestId={}, timeout={}s", lockKey, requestId, timeout.getSeconds());
            } else {
                log.debug("获取分布式锁失败（已被占用）: key={}", lockKey);
            }
            return acquired;
        } catch (Exception e) {
            log.error("获取分布式锁异常: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁（原子操作）
     * 使用 Lua 脚本确保 GET + DELETE 的原子性，防止如下竞态条件：
     * 1. 线程A的锁过期自动释放
     * 2. 线程B获取到锁
     * 3. 线程A的 unlock 误删线程B的锁
     *
     * @param key       锁的业务键
     * @param requestId 请求标识，必须与加锁时一致
     * @return 是否成功释放锁
     */
    public boolean unlock(String key, String requestId) {
        String lockKey = LOCK_PREFIX + key;
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, List.of(lockKey), requestId);
            boolean released = result != null && result > 0;
            if (released) {
                log.debug("成功释放分布式锁: key={}, requestId={}", lockKey, requestId);
            } else {
                log.warn("释放分布式锁失败（锁不存在或已被其他持有者释放）: key={}, requestId={}", lockKey, requestId);
            }
            return released;
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 生成全局唯一的请求标识
     */
    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
