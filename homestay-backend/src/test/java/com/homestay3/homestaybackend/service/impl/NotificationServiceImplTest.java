package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.event.NotificationCreatedEvent;
import com.homestay3.homestaybackend.event.NotificationUnreadCountChangedEvent;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.NotificationRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.NotificationPreferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HomestayRepository homestayRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(notificationPreferenceService.isEnabled(any(), any())).thenReturn(true);
    }

    @Test
    void createNotificationNormalizesLegacyOrderStatusTypeBeforeSaving() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(1L, "ROLE_USER")));

        notificationService.createNotification(
                1L,
                null,
                NotificationType.PAID,
                EntityType.ORDER,
                "99",
                "payment received");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationType.PAYMENT_RECEIVED, captor.getValue().getType());
        assertEquals(EntityType.ORDER, captor.getValue().getEntityType());

        NotificationDTO dto = captureCreatedNotificationEvent(1L);
        assertEquals("order", dto.getCategory());
        assertEquals("付款已收到", dto.getTitle());
        assertEquals("/orders/99", dto.getDeepLink());
    }

    @Test
    void createNotificationKeepsCanonicalTypeBeforeSaving() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(1L, "ROLE_HOST")));

        notificationService.createNotification(
                1L,
                null,
                NotificationType.ORDER_CONFIRMED,
                EntityType.ORDER,
                "99",
                "order confirmed");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationType.ORDER_CONFIRMED, captor.getValue().getType());

        NotificationDTO dto = captureCreatedNotificationEvent(1L);
        assertEquals("order", dto.getCategory());
        assertEquals("订单已确认", dto.getTitle());
        assertEquals("/host/orders?highlightOrderId=99", dto.getDeepLink());
    }

    @Test
    void createNotificationUsesUnknownWhenTypeIsNull() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(1L, "ROLE_USER")));

        notificationService.createNotification(
                1L,
                null,
                null,
                null,
                null,
                "missing type");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationType.UNKNOWN, captor.getValue().getType());
    }

    @Test
    void createNotificationNormalizesLegacyMessageEntityBeforeSaving() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(any())).thenReturn(List.of(user(1L, "ROLE_USER")));

        notificationService.createNotification(
                1L,
                null,
                NotificationType.NEW_MESSAGE,
                EntityType.MESSAGE,
                "thread-1",
                "new message");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(EntityType.MESSAGE_THREAD, captor.getValue().getEntityType());

        NotificationDTO dto = captureCreatedNotificationEvent(1L);
        assertEquals("message", dto.getCategory());
        assertEquals("/user/notifications", dto.getDeepLink());
    }

    @Test
    void broadcastSystemNotificationBatchSavesEnabledUsersAndPublishesEvents() {
        User user1 = user(1L, "ROLE_USER");
        User user2 = user(2L, "ROLE_USER");
        User user3 = user(3L, "ROLE_HOST");
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
        when(userRepository.findAllById(any())).thenReturn(List.of(user1, user3));
        when(notificationRepository.countUnreadByUserIds(any()))
                .thenReturn(List.of(new Object[]{1L, 4L}, new Object[]{3L, 8L}));

        notificationService.broadcastSystemNotification("system notice");

        verify(notificationRepository).saveAll(argThat(this::containsOnlyEnabledSystemNotifications));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(4)).publishEvent(eventCaptor.capture());

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
        assertEquals(Map.of(1L, 4L, 3L, 8L), unreadCounts);
    }

    @Test
    void markMultipleAsReadUsesBulkUpdateAndPublishesUnreadCount() {
        List<Long> notificationIds = List.of(10L, 11L);
        when(notificationRepository.markMultipleAsReadByUserId(42L, notificationIds)).thenReturn(2);
        when(notificationRepository.countByUserIdAndIsReadFalse(42L)).thenReturn(5L);

        int count = notificationService.markMultipleAsRead(notificationIds, 42L);

        assertEquals(2, count);
        verify(notificationRepository).markMultipleAsReadByUserId(42L, notificationIds);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NotificationUnreadCountChangedEvent event = (NotificationUnreadCountChangedEvent) eventCaptor.getValue();
        assertEquals(42L, event.userId());
        assertEquals(5L, event.unreadCount());
    }

    @Test
    void deleteMultipleNotificationsDeletesOwnedRowsAndPublishesUnreadCountWhenNeeded() {
        List<Long> notificationIds = List.of(10L, 11L);
        when(notificationRepository.countByUserIdAndIdInAndIsReadFalse(42L, notificationIds)).thenReturn(1L);
        when(notificationRepository.deleteByUserIdAndIdIn(42L, notificationIds)).thenReturn(2);
        when(notificationRepository.countByUserIdAndIsReadFalse(42L)).thenReturn(4L);

        int count = notificationService.deleteMultipleNotifications(notificationIds, 42L);

        assertEquals(2, count);
        verify(notificationRepository).deleteByUserIdAndIdIn(42L, notificationIds);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NotificationUnreadCountChangedEvent event = (NotificationUnreadCountChangedEvent) eventCaptor.getValue();
        assertEquals(42L, event.userId());
        assertEquals(4L, event.unreadCount());
    }

    private NotificationDTO captureCreatedNotificationEvent(Long expectedUserId) {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        NotificationCreatedEvent event = eventCaptor.getAllValues().stream()
                .filter(NotificationCreatedEvent.class::isInstance)
                .map(NotificationCreatedEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(expectedUserId, event.userId());
        return event.notification();
    }

    private boolean containsOnlyEnabledSystemNotifications(Iterable<Notification> notifications) {
        List<Notification> list = new ArrayList<>();
        notifications.forEach(list::add);
        return list.size() == 2
                && list.stream().map(Notification::getUserId).toList().equals(List.of(1L, 3L))
                && list.stream().allMatch(notification -> notification.getType() == NotificationType.SYSTEM_ANNOUNCEMENT)
                && list.stream().allMatch(notification -> notification.getEntityType() == EntityType.SYSTEM)
                && list.stream().allMatch(notification -> "system notice".equals(notification.getContent()));
    }

    private User user(Long id, String role) {
        return User.builder()
                .id(id)
                .username("user_" + id)
                .email("user_" + id + "@example.com")
                .password("secret")
                .role(role)
                .build();
    }
}
