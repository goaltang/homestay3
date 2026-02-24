package com.homestay3.homestaybackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 缓存管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
public class CacheController {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CacheController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 清理所有缓存
     */
    @PostMapping("/clear-all")
    public ResponseEntity<String> clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清理 {} 个缓存键", keys.size());
                return ResponseEntity.ok("已清理所有缓存，共 " + keys.size() + " 个键");
            } else {
                return ResponseEntity.ok("没有找到需要清理的缓存");
            }
        } catch (Exception e) {
            log.error("清理缓存失败", e);
            return ResponseEntity.internalServerError().body("清理缓存失败: " + e.getMessage());
        }
    }

    /**
     * 清理推荐相关缓存
     */
    @PostMapping("/clear-recommendation")
    public ResponseEntity<String> clearRecommendationCache() {
        try {
            int deletedCount = 0;
            
            // 清理推荐相关的缓存
            String[] cachePatterns = {
                "recommendedHomestays*",
                "popularHomestays*",
                "userRecommendations*",
                "recommendedHomestaysPage*",
                "popularHomestaysPage*"
            };
            
            for (String pattern : cachePatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    deletedCount += keys.size();
                    log.info("清理缓存模式 {} 下的 {} 个键", pattern, keys.size());
                }
            }
            
            return ResponseEntity.ok("已清理推荐相关缓存，共 " + deletedCount + " 个键");
        } catch (Exception e) {
            log.error("清理推荐缓存失败", e);
            return ResponseEntity.internalServerError().body("清理推荐缓存失败: " + e.getMessage());
        }
    }

    /**
     * 查看缓存统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<String> getCacheStats() {
        try {
            Set<String> allKeys = redisTemplate.keys("*");
            int totalKeys = allKeys != null ? allKeys.size() : 0;
            
            StringBuilder stats = new StringBuilder();
            stats.append("缓存统计信息:\n");
            stats.append("总缓存键数量: ").append(totalKeys).append("\n\n");
            
            // 统计不同类型的缓存
            String[] cacheTypes = {
                "recommendedHomestays", "popularHomestays", "userRecommendations",
                "homestayDetails", "searchResults"
            };
            
            for (String type : cacheTypes) {
                Set<String> typeKeys = redisTemplate.keys(type + "*");
                int count = typeKeys != null ? typeKeys.size() : 0;
                stats.append(type).append(": ").append(count).append(" 个键\n");
            }
            
            return ResponseEntity.ok(stats.toString());
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return ResponseEntity.internalServerError().body("获取缓存统计失败: " + e.getMessage());
        }
    }
} 