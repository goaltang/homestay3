package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.service.impl.OrderAutoStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单自动状态管理控制器
 * 提供订单自动状态流转的管理接口
 */
@RestController
@RequestMapping("/api/host/order-auto-status")
@RequiredArgsConstructor
@Slf4j
public class OrderAutoStatusController {
    
    private final OrderAutoStatusService orderAutoStatusService;
    
    /**
     * 获取自动状态流转统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HOST')")
    public ResponseEntity<Map<String, Object>> getAutoStatusStats() {
        try {
            OrderAutoStatusService.AutoStatusStats stats = orderAutoStatusService.getAutoStatusStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("pendingAutoCheckIn", stats.pendingAutoCheckIn);
            response.put("pendingAutoComplete", stats.pendingAutoComplete);
            response.put("missedCheckIn", stats.missedCheckIn);
            response.put("message", "获取自动状态统计成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取自动状态统计失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 手动触发自动状态流转
     * 用于测试或紧急情况下的手动执行
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HOST')")
    public ResponseEntity<Map<String, Object>> manualTriggerAutoStatus() {
        try {
            log.info("用户手动触发自动状态流转");
            orderAutoStatusService.manualTriggerAutoStatusTransition();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "自动状态流转已手动执行完成");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("手动触发自动状态流转失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "执行失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取自动状态流转配置信息
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HOST')")
    public ResponseEntity<Map<String, Object>> getAutoStatusConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("autoCheckInTime", "22:00");
        config.put("autoCheckOutTime", "06:00");
        config.put("cancelMissedCheckInTime", "12:00");
        config.put("checkInterval", "每小时");
        config.put("description", "系统自动状态流转配置");
        
        Map<String, String> rules = new HashMap<>();
        rules.put("自动入住", "入住日22:00后，已支付订单自动转为已入住");
        rules.put("自动完成", "退房日次日06:00后，已入住订单自动转为已完成");
        rules.put("错过入住处理", "入住日次日12:00后，未入住的已支付订单自动标记为已入住");
        
        config.put("rules", rules);
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "OrderAutoStatusService");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 调试接口：检查当前用户的权限信息
     */
    @GetMapping("/debug/user-info")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        
        if (authentication != null) {
            userInfo.put("principal", authentication.getPrincipal().toString());
            userInfo.put("authorities", authentication.getAuthorities().toString());
            userInfo.put("name", authentication.getName());
            userInfo.put("isAuthenticated", authentication.isAuthenticated());
        } else {
            userInfo.put("error", "No authentication found");
        }
        
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 分析历史订单状态，找出需要修复的订单
     */
    @GetMapping("/analyze-history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HOST')")
    public ResponseEntity<Map<String, Object>> analyzeHistoryOrders() {
        try {
            Map<String, Object> analysis = orderAutoStatusService.analyzeHistoryOrders();
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("分析历史订单失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "分析失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 修复历史订单状态
     */
    @PostMapping("/fix-history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HOST')")
    public ResponseEntity<Map<String, Object>> fixHistoryOrders() {
        try {
            Map<String, Object> result = orderAutoStatusService.fixHistoryOrders();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("修复历史订单失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "修复失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 