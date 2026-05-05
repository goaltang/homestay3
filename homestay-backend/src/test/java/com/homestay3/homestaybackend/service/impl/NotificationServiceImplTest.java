package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.NotificationRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createNotificationNormalizesLegacyOrderStatusTypeBeforeSaving() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "ROLE_USER")));

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

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(webSocketNotificationService).sendNotificationToUser(eq(1L), dtoCaptor.capture());
        assertEquals("order", dtoCaptor.getValue().getCategory());
        assertEquals("付款已收到", dtoCaptor.getValue().getTitle());
        assertEquals("/orders/99", dtoCaptor.getValue().getDeepLink());
    }

    @Test
    void createNotificationKeepsCanonicalTypeBeforeSaving() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "ROLE_HOST")));

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

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(webSocketNotificationService).sendNotificationToUser(eq(1L), dtoCaptor.capture());
        assertEquals("order", dtoCaptor.getValue().getCategory());
        assertEquals("订单已确认", dtoCaptor.getValue().getTitle());
        assertEquals("/host/orders?highlightOrderId=99", dtoCaptor.getValue().getDeepLink());
    }

    @Test
    void createNotificationUsesUnknownWhenTypeIsNull() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "ROLE_USER")));

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
