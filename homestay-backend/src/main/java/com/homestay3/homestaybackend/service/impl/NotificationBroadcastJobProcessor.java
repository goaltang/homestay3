package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.event.NotificationCreatedEvent;
import com.homestay3.homestaybackend.event.NotificationUnreadCountChangedEvent;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.NotificationBroadcastJobRepository;
import com.homestay3.homestaybackend.repository.NotificationRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.NotificationPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationBroadcastJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastJobProcessor.class);
    private static final int FAILURE_REASON_LIMIT = 1000;

    private final NotificationBroadcastJobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationBroadcastJobProcessor(NotificationBroadcastJobRepository jobRepository,
                                             NotificationRepository notificationRepository,
                                             UserRepository userRepository,
                                             NotificationPreferenceService notificationPreferenceService,
                                             ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationPreferenceService = notificationPreferenceService;
        this.eventPublisher = eventPublisher;
    }

    @Async("taskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void process(Long jobId, String content) {
        NotificationBroadcastJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Notification broadcast job {} not found", jobId);
            return;
        }

        NotificationBroadcastJob.Status status = job.getStatus();
        if (status == NotificationBroadcastJob.Status.SUCCEEDED
                || status == NotificationBroadcastJob.Status.FAILED
                || status == NotificationBroadcastJob.Status.RUNNING) {
            log.info("Notification broadcast job {} already in terminal or running status {}, skipping", jobId, status);
            return;
        }

        markRunning(job);
        int targetCount = 0;
        int successCount = 0;

        try {
            List<User> allUsers = userRepository.findAll();
            List<Long> userIds = allUsers.stream()
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .toList();

            Map<Long, Boolean> enabledByUserId = notificationPreferenceService.getEnabledMap(
                    userIds,
                    NotificationType.SYSTEM_ANNOUNCEMENT.getDomain());

            List<Notification> notifications = allUsers.stream()
                    .filter(user -> user.getId() != null)
                    .filter(user -> enabledByUserId.getOrDefault(user.getId(), true))
                    .map(user -> Notification.builder()
                            .userId(user.getId())
                            .actorId(job.getInitiatedBy())
                            .type(NotificationType.SYSTEM_ANNOUNCEMENT)
                            .entityType(EntityType.SYSTEM)
                            .entityId(null)
                            .content(content)
                            .isRead(false)
                            .build())
                    .toList();

            targetCount = notifications.size();
            job.setTargetCount(targetCount);

            if (!notifications.isEmpty()) {
                List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
                successCount = savedNotifications.size();
                publishNotificationEvents(savedNotifications);
                publishUnreadCountChangedBatch(savedNotifications.stream()
                        .map(Notification::getUserId)
                        .toList());
            }

            complete(job, targetCount, successCount);
            log.info("Notification broadcast job {} completed, targetCount={}, successCount={}",
                    jobId, targetCount, successCount);
        } catch (Exception ex) {
            fail(job, targetCount, successCount, ex);
            log.warn("Notification broadcast job {} failed: {}", jobId, ex.getMessage(), ex);
        }
    }

    private void markRunning(NotificationBroadcastJob job) {
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(NotificationBroadcastJob.Status.RUNNING);
        job.setStartedAt(now);
        job.setUpdatedAt(now);
        jobRepository.save(job);
    }

    private void complete(NotificationBroadcastJob job, int targetCount, int successCount) {
        LocalDateTime now = LocalDateTime.now();
        int failureCount = Math.max(targetCount - successCount, 0);
        job.setTargetCount(targetCount);
        job.setSuccessCount(successCount);
        job.setFailureCount(failureCount);
        job.setFailureReason(failureCount > 0 ? "Not all notifications were created." : null);
        job.setStatus(failureCount > 0
                ? NotificationBroadcastJob.Status.FAILED
                : NotificationBroadcastJob.Status.SUCCEEDED);
        job.setCompletedAt(now);
        job.setUpdatedAt(now);
        jobRepository.save(job);
    }

    private void fail(NotificationBroadcastJob job, int targetCount, int successCount, Exception ex) {
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(NotificationBroadcastJob.Status.FAILED);
        job.setTargetCount(targetCount);
        job.setSuccessCount(successCount);
        job.setFailureCount(Math.max(targetCount - successCount, 0));
        job.setFailureReason(truncate(resolveFailureReason(ex), FAILURE_REASON_LIMIT));
        job.setCompletedAt(now);
        job.setUpdatedAt(now);
        jobRepository.save(job);
    }

    private void publishNotificationEvents(List<Notification> notifications) {
        for (Notification notification : notifications) {
            NotificationDTO dto = toBroadcastDto(notification);
            eventPublisher.publishEvent(new NotificationCreatedEvent(notification.getUserId(), dto));
        }
    }

    private NotificationDTO toBroadcastDto(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setActorId(notification.getActorId());
        dto.setType(notification.getType());
        dto.setRawType(notification.getType() != null ? notification.getType().name() : null);
        dto.setEntityType(notification.getEntityType());
        dto.setRawEntityType(notification.getEntityType() != null ? notification.getEntityType().name() : null);
        dto.setEntityId(notification.getEntityId());
        dto.setContent(notification.getContent());
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setCategory(NotificationType.SYSTEM_ANNOUNCEMENT.getDomain().getCategory());
        dto.setTitle(NotificationType.SYSTEM_ANNOUNCEMENT.getDefaultTitle());
        dto.setPayload(buildPayload(dto));
        return dto;
    }

    private Map<String, Object> buildPayload(NotificationDTO dto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfNotNull(payload, "rawType", dto.getRawType());
        putIfNotNull(payload, "rawEntityType", dto.getRawEntityType());
        putIfNotNull(payload, "entityType", dto.getEntityType() != null ? dto.getEntityType().name() : null);
        putIfNotNull(payload, "entityId", dto.getEntityId());
        putIfNotNull(payload, "actorId", dto.getActorId());
        return payload;
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private void publishUnreadCountChangedBatch(Collection<Long> userIds) {
        Set<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (distinctUserIds.isEmpty()) {
            return;
        }

        Map<Long, Long> unreadCountByUserId = notificationRepository.countUnreadByUserIds(distinctUserIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()));

        for (Long userId : distinctUserIds) {
            eventPublisher.publishEvent(new NotificationUnreadCountChangedEvent(
                    userId,
                    unreadCountByUserId.getOrDefault(userId, 0L)));
        }
    }

    private String resolveFailureReason(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
