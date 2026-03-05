package com.homestay3.homestaybackend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@Slf4j
public class CacheConfig {

    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存管理器 - 优先尝试使用 Redis，如果 Redis 不可用则降级为内存缓存
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            // 先测试 Redis 连接
            connectionFactory.getConnection().ping();
            log.info("Redis 连接成功，使用 Redis 缓存管理器");
            return createRedisCacheManager(connectionFactory);
        } catch (Exception e) {
            log.warn("Redis 连接失败（{}），降级为内存缓存。支付等核心功能不受影响。", e.getMessage());
            return new ConcurrentMapCacheManager(
                    "recommendedHomestays", "recommendedHomestaysPage",
                    "popularHomestays", "popularHomestaysPage",
                    "homestayDetails", "searchResults", "userRecommendations");
        }
    }

    private RedisCacheManager createRedisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = createCacheConfiguration(cacheProperties.getRedis().getTimeToLive());

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 推荐民宿缓存 - 30分钟
        cacheConfigurations.put("recommendedHomestays", createCacheConfiguration(Duration.ofMinutes(30)));
        cacheConfigurations.put("recommendedHomestaysPage", createCacheConfiguration(Duration.ofMinutes(30)));

        // 热门民宿缓存 - 15分钟
        cacheConfigurations.put("popularHomestays", createCacheConfiguration(Duration.ofMinutes(15)));
        cacheConfigurations.put("popularHomestaysPage", createCacheConfiguration(Duration.ofMinutes(15)));

        // 民宿详情缓存 - 10分钟
        cacheConfigurations.put("homestayDetails", createCacheConfiguration(Duration.ofMinutes(10)));

        // 搜索结果缓存 - 5分钟
        cacheConfigurations.put("searchResults", createCacheConfiguration(Duration.ofMinutes(5)));

        // 用户推荐缓存 - 1小时
        cacheConfigurations.put("userRecommendations", createCacheConfiguration(Duration.ofHours(1)));

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }
}