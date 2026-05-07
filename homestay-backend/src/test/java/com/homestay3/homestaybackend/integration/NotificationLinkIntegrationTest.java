package com.homestay3.homestaybackend.integration;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationDomain;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.service.NotificationPreferenceService;
import com.homestay3.homestaybackend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class NotificationLinkIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Test
    void createNotificationAppliesPreferenceFilterAndListQueryReturnsPersistedNotifications() {
        Long userId = uniqueUserId();

        Notification saved = notificationService.createNotification(
                userId,
                null,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                EntityType.SYSTEM,
                null,
                "allowed system notice");

        notificationPreferenceService.updatePreference(userId, NotificationDomain.MESSAGE, false);

        Notification filtered = notificationService.createNotification(
                userId,
                null,
                NotificationType.NEW_MESSAGE,
                EntityType.MESSAGE_THREAD,
                "thread-42",
                "filtered message notice");

        Page<NotificationDTO> notifications = notificationService.getNotificationsForUser(
                userId,
                null,
                null,
                PageRequest.of(0, 10));

        assertThat(saved).isNotNull();
        assertThat(filtered).isNull();
        assertThat(notifications.getTotalElements()).isEqualTo(1);
        assertThat(notifications.getContent())
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getId()).isEqualTo(saved.getId());
                    assertThat(notification.getUserId()).isEqualTo(userId);
                    assertThat(notification.getType()).isEqualTo(NotificationType.SYSTEM_ANNOUNCEMENT);
                    assertThat(notification.getEntityType()).isEqualTo(EntityType.SYSTEM);
                    assertThat(notification.getContent()).isEqualTo("allowed system notice");
                    assertThat(notification.isRead()).isFalse();
                });
        assertThat(notificationService.getUnreadNotificationCount(userId)).isEqualTo(1);
    }

    private Long uniqueUserId() {
        return 9_000_000L + Math.floorMod(System.nanoTime(), 1_000_000L);
    }
}
