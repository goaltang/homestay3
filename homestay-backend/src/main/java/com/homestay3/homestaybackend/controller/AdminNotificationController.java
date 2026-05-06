package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationController.class);

    private final NotificationService notificationService;

    @Autowired
    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 向全体用户广播系统通知
     * @param body 请求体 { "content": "..." }
     * @return 任务提交结果
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcastSystemNotification(
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        notificationService.broadcastSystemNotification(content.trim());
        log.info("Admin 提交系统通知广播任务，内容长度={}", content.length());
        return ResponseEntity.accepted().body(Map.of("status", "submitted"));
    }

    /**
     * 向指定用户发送系统通知
     * @param body 请求体 { "userId": 123, "content": "..." }
     * @return 创建的通知详情
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationDTO> sendSystemNotificationToUser(
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String content = (String) body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        NotificationDTO dto = notificationService.sendSystemNotification(userId, content.trim());
        if (dto == null) {
            // 用户可能关闭了系统通知（理论上不应发生，因为 system 强制开启）
            return ResponseEntity.ok().build();
        }
        log.info("Admin 向用户 {} 发送系统通知", userId);
        return ResponseEntity.ok(dto);
    }
}
