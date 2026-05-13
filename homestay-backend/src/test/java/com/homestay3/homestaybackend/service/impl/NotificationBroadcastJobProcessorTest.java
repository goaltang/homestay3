package com.homestay3.homestaybackend.service.impl;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationBroadcastJobProcessorTest {

    @Mock
    private NotificationBroadcastJobRepository jobRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationBroadcastJobProcessor processor;

    @Test
    void processSavesEnabledUserNotificationsAndCompletesJob() {
        NotificationBroadcastJob job = pendingJob(20L);
        User user1 = user(1L);
        User user2 = user(2L);
        User user3 = user(3L);

        when(jobRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(NotificationBroadcastJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of(user1, user2, user3));
        when(notificationPreferenceService.getEnabledMap(any(), any()))
                .thenReturn(Map.of(1L, true, 2L, false, 3L, true));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Notification> notifications = invocation.getArgument(0);
            List<Notification> saved = new ArrayList<>();
            long id = 1L;
            for (Notification notification : notifications) {
                notification.setId(id++);
                saved.add(notification);
            }
            return saved;
        });
        when(notificationRepository.countUnreadByUserIds(any()))
                .thenReturn(List.of(new Object[]{1L, 5L}, new Object[]{3L, 7L}));

        processor.process(20L, "system notice");

        assertEquals(NotificationBroadcastJob.Status.SUCCEEDED, job.getStatus());
        assertEquals(2, job.getTargetCount());
        assertEquals(2, job.getSuccessCount());
        assertEquals(0, job.getFailureCount());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getCompletedAt());

        verify(notificationRepository).saveAll(argThat(this::containsEnabledSystemNotifications));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(4)).publishEvent(eventCaptor.capture());
        List<Long> createdUserIds = eventCaptor.getAllValues().stream()
                .filter(NotificationCreatedEvent.class::isInstance)
                .map(NotificationCreatedEvent.class::cast)
                .map(NotificationCreatedEvent::userId)
                .toList();
        assertEquals(List.of(1L, 3L), createdUserIds);

        Map<Long, Long> unreadCounts = eventCaptor.getAllValues().stream()
                .filter(NotificationUnreadCountChangedEvent.class::isInstance)
                .map(NotificationUnreadCountChangedEvent.class::cast)
                .collect(Collectors.toMap(
                        NotificationUnreadCountChangedEvent::userId,
                        NotificationUnreadCountChangedEvent::unreadCount));
        assertEquals(Map.of(1L, 5L, 3L, 7L), unreadCounts);
    }

    @Test
    void processRecordsFailureReasonWhenSaveAllFails() {
        NotificationBroadcastJob job = pendingJob(21L);
        when(jobRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(NotificationBroadcastJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of(user(1L), user(2L)));
        when(notificationPreferenceService.getEnabledMap(any(), any()))
                .thenReturn(Map.of(1L, true, 2L, true));
        when(notificationRepository.saveAll(any()))
                .thenThrow(new RuntimeException("database unavailable"));

        processor.process(21L, "system notice");

        assertEquals(NotificationBroadcastJob.Status.FAILED, job.getStatus());
        assertEquals(2, job.getTargetCount());
        assertEquals(0, job.getSuccessCount());
        assertEquals(2, job.getFailureCount());
        assertEquals("database unavailable", job.getFailureReason());
        assertNotNull(job.getCompletedAt());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private boolean containsEnabledSystemNotifications(Iterable<Notification> notifications) {
        List<Notification> list = new ArrayList<>();
        notifications.forEach(list::add);
        return list.size() == 2
                && list.stream().map(Notification::getUserId).toList().equals(List.of(1L, 3L))
                && list.stream().allMatch(notification -> notification.getActorId().equals(7L))
                && list.stream().allMatch(notification -> notification.getType() == NotificationType.SYSTEM_ANNOUNCEMENT)
                && list.stream().allMatch(notification -> notification.getEntityType() == EntityType.SYSTEM)
                && list.stream().allMatch(notification -> "system notice".equals(notification.getContent()));
    }

    private NotificationBroadcastJob pendingJob(Long id) {
        return NotificationBroadcastJob.builder()
                .id(id)
                .status(NotificationBroadcastJob.Status.PENDING)
                .initiatedBy(7L)
                .contentSummary("system notice")
                .contentLength(13)
                .submittedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .username("user_" + id)
                .email("user_" + id + "@example.com")
                .password("secret")
                .role("ROLE_USER")
                .build();
    }
}
