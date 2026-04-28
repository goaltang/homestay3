package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.service.search.UserBehaviorTrackingService;
import com.homestay3.homestaybackend.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 用户行为埋点接口
 * 前端在关键交互点异步调用，用于收集用户偏好数据
 */
@Slf4j
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class BehaviorTrackingController {

    private final UserBehaviorTrackingService trackingService;

    /**
     * 记录房源浏览
     */
    @PostMapping("/view/{homestayId}")
    public ResponseEntity<?> trackView(
            @PathVariable Long homestayId,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        try {
            Long userId = resolveUserId(request);
            String sessionId = request.getSession().getId();
            String cityCode = (String) payload.get("cityCode");
            String type = (String) payload.get("type");
            BigDecimal price = payload.get("price") != null
                    ? new BigDecimal(payload.get("price").toString())
                    : null;

            trackingService.trackView(userId, sessionId, homestayId, cityCode, type, price);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.debug("Track view failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    /**
     * 记录搜索行为
     */
    @PostMapping("/search")
    public ResponseEntity<?> trackSearch(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        try {
            Long userId = resolveUserId(request);
            String sessionId = request.getSession().getId();
            String keyword = (String) payload.get("keyword");
            String cityCode = (String) payload.get("cityCode");
            String type = (String) payload.get("type");

            trackingService.trackSearch(userId, sessionId, keyword, cityCode, type);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.debug("Track search failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    /**
     * 记录点击行为
     */
    @PostMapping("/click/{homestayId}")
    public ResponseEntity<?> trackClick(
            @PathVariable Long homestayId,
            HttpServletRequest request) {
        try {
            Long userId = resolveUserId(request);
            String sessionId = request.getSession().getId();

            trackingService.trackClick(userId, sessionId, homestayId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.debug("Track click failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        // 优先从 SecurityContext 获取当前认证用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof com.homestay3.homestaybackend.entity.User) {
                return ((com.homestay3.homestaybackend.entity.User) principal).getId();
            }
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                // username 可能是用户 ID 或用户名，尝试解析为 Long
                try {
                    return Long.valueOf(username);
                } catch (NumberFormatException ignored) {}
            }
        }

        // 降级：从 request attribute 获取（某些过滤器可能设置）
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        if (userIdAttr instanceof String) {
            try {
                return Long.valueOf((String) userIdAttr);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
