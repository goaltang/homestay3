package com.homestay3.homestaybackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 健康检查组件
 * 用于检测 Redis 连接是否可用，供其他组件在调用 Redis 前进行降级判断
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查 Redis 是否可用
     * 通过执行 PING 命令测试连接状态
     *
     * @return true 如果 Redis 连接正常，false 如果不可用
     */
    public boolean isRedisAvailable() {
        try {
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equalsIgnoreCase(result);
        } catch (Exception e) {
            log.warn("Redis 连接不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Redis 连接信息
     *
     * @return Redis 服务器信息，如不可用则返回 "unavailable"
     */
    public String getRedisInfo() {
        try {
            return redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info().toString();
        } catch (Exception e) {
            log.warn("无法获取 Redis 信息: {}", e.getMessage());
            return "unavailable";
        }
    }
}
