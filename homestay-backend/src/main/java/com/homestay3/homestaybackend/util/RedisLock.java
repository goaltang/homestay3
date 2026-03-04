package com.homestay3.homestaybackend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";

    public boolean tryLock(String key, String requestId, Duration timeout) {
        String lockKey = LOCK_PREFIX + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, timeout);
        return Boolean.TRUE.equals(success);
    }

    public void unlock(String key, String requestId) {
        String lockKey = LOCK_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(lockKey);
        if (requestId.equals(value)) {
            redisTemplate.delete(lockKey);
        }
    }

    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
