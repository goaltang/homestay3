package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.model.enums.NotificationDomain;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.service.NotificationPreferenceService;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @Autowired
    public NotificationController(NotificationService notificationService,
                                   NotificationPreferenceService preferenceService) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
    }

    private Long getCurrentUserId() {
        return UserUtil.getCurrentUserId();
    }

    /**
     * 获取当前用户的通知列表 (分页)
     * @param isRead 是否只看已读/未读 (可选)
     * @param type 通知类型 (可选, 传入枚举名称字符串)
     * @param pageable 分页参数 (默认按创建时间降序)
     * @return 通知分页结果
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getMyNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = getCurrentUserId();
        NotificationType notificationType = parseTypeFilter(type);
        log.info("用户 ID {} 请求通知列表, isRead={}, type={}, page={}, size={}", 
                 currentUserId, isRead, notificationType, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationDTO> notificationPage = notificationService.getNotificationsForUser(currentUserId, isRead, notificationType, pageable);
        
        return ResponseEntity.ok(notificationPage);
    }

    private NotificationType parseTypeFilter(String type) {
        NotificationType parsedType = NotificationType.parseFilterValue(type);
        if (parsedType == null && type != null && !type.trim().isEmpty()) {
            log.warn("Ignoring invalid notification type filter: {}", type);
        }
        return parsedType;
    }

    /**
     * 获取当前用户的未读通知数量
     * @return 未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getMyUnreadNotificationCount() {
        Long currentUserId = getCurrentUserId();
        long count = notificationService.getUnreadNotificationCount(currentUserId);
        log.info("用户 ID {} 请求未读通知数量, 结果: {}", currentUserId, count);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 将指定通知标记为已读
     * @param notificationId 通知ID
     * @return 更新后的通知详情
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDTO> markNotificationAsRead(@PathVariable Long notificationId) {
        Long currentUserId = getCurrentUserId();
        log.info("用户 ID {} 尝试标记通知 {} 为已读", currentUserId, notificationId);
        NotificationDTO notification = notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(notification);
    }

    /**
     * 批量将多条通知标记为已读
     * @param notificationIds 通知ID列表
     * @return 成功标记的通知数量
     */
    @PostMapping("/read-multiple")
    public ResponseEntity<Map<String, Integer>> markMultipleNotificationsAsRead(@RequestBody List<Long> notificationIds) {
        Long currentUserId = getCurrentUserId();
        log.info("用户 ID {} 尝试批量标记通知 {} 为已读", currentUserId, notificationIds);
        int count = notificationService.markMultipleAsRead(notificationIds, currentUserId);
        Map<String, Integer> response = new HashMap<>();
        response.put("markedCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 将当前用户的所有未读通知标记为已读
     * @return 包含标记数量的响应
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllNotificationsAsRead() {
        Long currentUserId = getCurrentUserId();
        log.info("用户 ID {} 尝试标记所有未读通知为已读", currentUserId);
        int updatedCount = notificationService.markAllAsRead(currentUserId);
        Map<String, Integer> response = new HashMap<>();
        response.put("markedCount", updatedCount);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除指定通知 (可选)
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        Long currentUserId = getCurrentUserId();
        log.info("用户 ID {} 尝试删除通知 {}", currentUserId, notificationId);
        notificationService.deleteNotification(notificationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取当前用户的通知偏好设置
     * @return 各业务域的开启状态
     */
    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Boolean>> getNotificationPreferences() {
        Long currentUserId = getCurrentUserId();
        Map<NotificationDomain, Boolean> prefs = preferenceService.getPreferences(currentUserId);
        Map<String, Boolean> response = new LinkedHashMap<>();
        prefs.forEach((domain, enabled) -> response.put(domain.name().toLowerCase(), enabled));
        return ResponseEntity.ok(response);
    }

    /**
     * 更新指定业务域的通知偏好
     * @param domain 业务域名称（小写）
     * @param body 请求体 { "enabled": true/false }
     * @return 操作结果
     */
    @PutMapping("/preferences/{domain}")
    public ResponseEntity<Void> updateNotificationPreference(
            @PathVariable String domain,
            @RequestBody Map<String, Boolean> body) {
        Long currentUserId = getCurrentUserId();
        NotificationDomain notificationDomain = NotificationDomain.valueOf(domain.toUpperCase());
        boolean enabled = body.getOrDefault("enabled", true);
        preferenceService.updatePreference(currentUserId, notificationDomain, enabled);
        log.info("用户 {} 更新通知偏好: domain={}, enabled={}", currentUserId, notificationDomain, enabled);
        return ResponseEntity.ok().build();
    }
} 
