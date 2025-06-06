package com.homestay3.homestaybackend.controller;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * 系统相关API控制器
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class SystemController {
    
    /**
     * 获取服务器当前时间戳
     * 用于前端时间同步
     */
    @GetMapping("/time")
    public Map<String, Object> getServerTime() {
        return Map.of(
            "timestamp", System.currentTimeMillis(),
            "iso", Instant.now().toString()
        );
    }
    
    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        );
    }
} 