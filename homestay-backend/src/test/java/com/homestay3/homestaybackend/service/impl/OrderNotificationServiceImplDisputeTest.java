package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderNotificationService 争议通知相关测试
 */
@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceImplDisputeTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderNotificationServiceImpl notificationServiceImpl;

    private static final Long ORDER_ID = 1L;
    private static final Long GUEST_ID = 100L;
    private static final Long HOST_ID = 200L;
    private static final String ORDER_NUMBER = "ORDER202403130001";
    private static final String HOMESTAY_TITLE = "温馨民宿";
    private static final String DISPUTE_REASON = "房东不同意全额退款";
    private static final String RESOLUTION_NOTE = "仲裁备注";

    @BeforeEach
    void setUp() {
        // 通用mock行为
    }

    // ========== sendDisputeRaisedNotification 测试 ==========

    @Test
    void sendDisputeRaisedNotification_WithBothUsers_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));
        when(userRepository.findById(HOST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when
        notificationServiceImpl.sendDisputeRaisedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, DISPUTE_REASON);

        // then
        // 验证房东收到通知
        ArgumentCaptor<String> hostContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(HOST_ID), eq(GUEST_ID), eq(NotificationType.REFUND_REQUESTED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), hostContentCaptor.capture());
        assertTrue(hostContentCaptor.getValue().contains("争议待处理"));
        assertTrue(hostContentCaptor.getValue().contains(DISPUTE_REASON));

        // 验证客人收到通知
        ArgumentCaptor<String> guestContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(GUEST_ID), eq(HOST_ID), eq(NotificationType.REFUND_REQUESTED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), guestContentCaptor.capture());
        assertTrue(guestContentCaptor.getValue().contains("争议处理流程"));
        assertTrue(guestContentCaptor.getValue().contains("仲裁"));
    }

    @Test
    void sendDisputeRaisedNotification_WithOnlyGuest_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when
        notificationServiceImpl.sendDisputeRaisedNotification(
                ORDER_ID, GUEST_ID, null, ORDER_NUMBER, HOMESTAY_TITLE, DISPUTE_REASON);

        // then
        // 只应发送一个通知给客人
        verify(notificationService, times(1)).createNotification(
                eq(GUEST_ID), isNull(), eq(NotificationType.REFUND_REQUESTED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), any());
    }

    @Test
    void sendDisputeRaisedNotification_NullReason_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));
        when(userRepository.findById(HOST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when
        notificationServiceImpl.sendDisputeRaisedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, null);

        // then
        verify(notificationService, times(2)).createNotification(
                any(), any(), eq(NotificationType.REFUND_REQUESTED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), any());
    }

    // ========== sendDisputeResolvedNotification 测试 ==========

    @Test
    void sendDisputeResolvedNotification_Approved_WithBothUsers_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));
        when(userRepository.findById(HOST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when
        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, "APPROVED", RESOLUTION_NOTE);

        // then
        // 验证房东收到通知
        ArgumentCaptor<String> hostContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(HOST_ID), eq(GUEST_ID), eq(NotificationType.ORDER_STATUS_CHANGED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), hostContentCaptor.capture());
        assertTrue(hostContentCaptor.getValue().contains("APPROVED") || hostContentCaptor.getValue().contains("批准"));

        // 验证客人收到通知
        ArgumentCaptor<String> guestContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(GUEST_ID), eq(HOST_ID), eq(NotificationType.REFUND_COMPLETED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), guestContentCaptor.capture());
        assertTrue(guestContentCaptor.getValue().contains("批准") || guestContentCaptor.getValue().contains("APPROVED"));
    }

    @Test
    void sendDisputeResolvedNotification_Rejected_WithBothUsers_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));
        when(userRepository.findById(HOST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when
        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, "REJECTED", RESOLUTION_NOTE);

        // then
        // 验证房东收到通知
        verify(notificationService).createNotification(
                eq(HOST_ID), eq(GUEST_ID), eq(NotificationType.ORDER_STATUS_CHANGED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), any());

        // 验证客人收到通知
        verify(notificationService).createNotification(
                eq(GUEST_ID), eq(HOST_ID), eq(NotificationType.REFUND_COMPLETED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), any());
    }

    @Test
    void sendDisputeResolvedNotification_WithOnlyGuest_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when
        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, null, ORDER_NUMBER, HOMESTAY_TITLE, "APPROVED", RESOLUTION_NOTE);

        // then
        // 只应发送一个通知给客人
        verify(notificationService, times(1)).createNotification(
                eq(GUEST_ID), isNull(), eq(NotificationType.REFUND_COMPLETED),
                eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), any());
    }

    @Test
    void sendDisputeResolvedNotification_NullNote_Success() {
        // given
        when(userRepository.findById(GUEST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));
        when(userRepository.findById(HOST_ID)).thenReturn(java.util.Optional.of(new com.homestay3.homestaybackend.entity.User()));

        // when - 传入null备注
        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, "APPROVED", null);

        // then - 不应抛出异常
        verify(notificationService, times(2)).createNotification(
                any(), any(), any(), eq(EntityType.ORDER), eq(String.valueOf(ORDER_ID)), any());
    }
}
