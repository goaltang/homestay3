package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.NotificationDto;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.utils.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Autowired
    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserIdByUsername() {
        String username = UserUtil.getCurrentUsername();
        if (username == null) {
            throw new IllegalStateException("无法获取当前用户名");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户 '" + username + "' 在数据库中未找到"));
        return user.getId();
    }

    /**
     * 获取当前用户的通知列表 (分页)
     * @param isRead 是否只看已读/未读 (可选)
     * @param type 通知类型 (可选, 传入枚举名称字符串)
     * @param pageable 分页参数 (默认按创建时间降序)
     * @return 通知分页结果
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = getCurrentUserIdByUsername();
        log.info("用户 ID {} (来自用户名 {}) 请求通知列表, isRead={}, type={}, page={}, size={}", 
                 currentUserId, UserUtil.getCurrentUsername(), isRead, type, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationDto> notificationPage = notificationService.getNotificationsForUser(currentUserId, isRead, type, pageable);
        
        return ResponseEntity.ok(notificationPage);
    }

    /**
     * 获取当前用户的未读通知数量
     * @return 未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getMyUnreadNotificationCount() {
        Long currentUserId = getCurrentUserIdByUsername();
        long count = notificationService.getUnreadNotificationCount(currentUserId);
        log.info("用户 ID {} (来自用户名 {}) 请求未读通知数量, 结果: {}", currentUserId, UserUtil.getCurrentUsername(), count);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 将指定通知标记为已读
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long notificationId) {
        Long currentUserId = getCurrentUserIdByUsername();
        log.info("用户 ID {} (来自用户名 {}) 尝试标记通知 {} 为已读", currentUserId, UserUtil.getCurrentUsername(), notificationId);
        boolean success = notificationService.markAsRead(notificationId, currentUserId);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * 将当前用户的所有未读通知标记为已读
     * @return 包含标记数量的响应
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllNotificationsAsRead() {
        Long currentUserId = getCurrentUserIdByUsername();
        log.info("用户 ID {} (来自用户名 {}) 尝试标记所有未读通知为已读", currentUserId, UserUtil.getCurrentUsername());
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
        Long currentUserId = getCurrentUserIdByUsername();
        log.info("用户 ID {} (来自用户名 {}) 尝试删除通知 {}", currentUserId, UserUtil.getCurrentUsername(), notificationId);
        notificationService.deleteNotification(notificationId, currentUserId);
        return ResponseEntity.noContent().build();
    }
} 