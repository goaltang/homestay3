package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;
import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import com.homestay3.homestaybackend.service.NotificationBroadcastService;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationController.class);

    private final NotificationService notificationService;
    private final NotificationBroadcastService notificationBroadcastService;

    @Autowired
    public AdminNotificationController(NotificationService notificationService,
                                       NotificationBroadcastService notificationBroadcastService) {
        this.notificationService = notificationService;
        this.notificationBroadcastService = notificationBroadcastService;
    }

    /**
     * 向全体用户广播系统通知
     * @param body 请求体 { "content": "..." }
     * @return 任务提交结果
     */
    @PostMapping("/broadcast")
    public ResponseEntity<NotificationBroadcastJobDTO> broadcastSystemNotification(
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        NotificationBroadcastJobDTO job = notificationBroadcastService.submitBroadcast(
                content.trim(),
                currentUserIdOrNull(),
                UserUtil.getCurrentUsername());
        log.info("Admin submitted notification broadcast job {}, status={}, contentLength={}",
                job.getJobId(), job.getStatus(), job.getContentLength());
        if (job.getStatus() == NotificationBroadcastJob.Status.RATE_LIMITED) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(job);
        }
        return ResponseEntity.accepted().body(job);
    }

    @GetMapping("/broadcast-jobs")
    public ResponseEntity<Page<NotificationBroadcastJobDTO>> getBroadcastJobs(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        NotificationBroadcastJob.Status statusFilter = parseBroadcastJobStatus(status);
        Page<NotificationBroadcastJobDTO> jobs = notificationBroadcastService.getBroadcastJobs(statusFilter, pageable);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/broadcast-jobs/{jobId}")
    public ResponseEntity<NotificationBroadcastJobDTO> getBroadcastJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(notificationBroadcastService.getBroadcastJob(jobId));
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

    private NotificationBroadcastJob.Status parseBroadcastJobStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return NotificationBroadcastJob.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid broadcast job status: " + status);
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return UserUtil.getCurrentUserId();
        } catch (IllegalStateException ex) {
            log.debug("Unable to resolve current admin user id for notification broadcast audit: {}", ex.getMessage());
            return null;
        }
    }
}
