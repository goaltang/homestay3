package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.util.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NotificationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupService.class);
    private static final String CLEANUP_LOCK_KEY = "notification:cleanup";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);

    private final NotificationService notificationService;
    private final RedisLock redisLock;

    @Value("${notification.cleanup.days:30}")
    private int cleanupDays;

    public NotificationCleanupService(NotificationService notificationService, RedisLock redisLock) {
        this.notificationService = notificationService;
        this.redisLock = redisLock;
    }

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupOldNotifications() {
        String requestId = redisLock.generateRequestId();
        boolean acquired = redisLock.tryLock(CLEANUP_LOCK_KEY, requestId, LOCK_TIMEOUT);
        if (!acquired) {
            log.info("通知清理任务已被其他实例执行，本次跳过");
            return;
        }

        try {
            log.info("开始清理过期通知...");
            int deletedCount = notificationService.cleanupOldNotifications(cleanupDays);
            log.info("通知清理完成，删除了 {} 条过期通知", deletedCount);
        } catch (Exception e) {
            log.error("清理通知时发生错误: {}", e.getMessage(), e);
        } finally {
            redisLock.unlock(CLEANUP_LOCK_KEY, requestId);
        }
    }
}
