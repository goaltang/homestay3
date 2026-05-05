package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationCreateCommand;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private static final String HOMESTAY_TITLE = "测试民宿";
    private static final String DISPUTE_REASON = "房东不同意全额退款";
    private static final String RESOLUTION_NOTE = "仲裁备注";

    @Test
    void sendDisputeRaisedNotificationWithBothUsersCreatesHostAndGuestCommands() {
        stubUser(GUEST_ID);
        stubUser(HOST_ID);

        notificationServiceImpl.sendDisputeRaisedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, DISPUTE_REASON);

        List<NotificationCreateCommand> commands = captureCreateCommands(2);
        NotificationCreateCommand hostCommand = findCommandForUser(commands, HOST_ID);
        assertCommand(hostCommand, HOST_ID, GUEST_ID, NotificationType.REFUND_REQUESTED);
        assertTrue(hostCommand.content().contains(DISPUTE_REASON));

        NotificationCreateCommand guestCommand = findCommandForUser(commands, GUEST_ID);
        assertCommand(guestCommand, GUEST_ID, HOST_ID, NotificationType.REFUND_REQUESTED);
        assertTrue(guestCommand.content().contains("争议处理流程"));
    }

    @Test
    void sendDisputeRaisedNotificationWithOnlyGuestCreatesOneCommand() {
        stubUser(GUEST_ID);

        notificationServiceImpl.sendDisputeRaisedNotification(
                ORDER_ID, GUEST_ID, null, ORDER_NUMBER, HOMESTAY_TITLE, DISPUTE_REASON);

        List<NotificationCreateCommand> commands = captureCreateCommands(1);
        assertCommand(commands.get(0), GUEST_ID, null, NotificationType.REFUND_REQUESTED);
    }

    @Test
    void sendDisputeRaisedNotificationWithNullReasonUsesFallbackContent() {
        stubUser(GUEST_ID);
        stubUser(HOST_ID);

        notificationServiceImpl.sendDisputeRaisedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, null);

        List<NotificationCreateCommand> commands = captureCreateCommands(2);
        assertTrue(findCommandForUser(commands, HOST_ID).content().contains("未提供"));
    }

    @Test
    void sendDisputeResolvedNotificationApprovedCreatesHostAndGuestCommands() {
        stubUser(GUEST_ID);
        stubUser(HOST_ID);

        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, "APPROVED", RESOLUTION_NOTE);

        List<NotificationCreateCommand> commands = captureCreateCommands(2);
        NotificationCreateCommand hostCommand = findCommandForUser(commands, HOST_ID);
        assertCommand(hostCommand, HOST_ID, GUEST_ID, NotificationType.ORDER_STATUS_CHANGED);
        assertTrue(hostCommand.content().contains("已批准退款"));

        NotificationCreateCommand guestCommand = findCommandForUser(commands, GUEST_ID);
        assertCommand(guestCommand, GUEST_ID, HOST_ID, NotificationType.REFUND_COMPLETED);
        assertTrue(guestCommand.content().contains(RESOLUTION_NOTE));
    }

    @Test
    void sendDisputeResolvedNotificationRejectedCreatesHostAndGuestCommands() {
        stubUser(GUEST_ID);
        stubUser(HOST_ID);

        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, "REJECTED", RESOLUTION_NOTE);

        List<NotificationCreateCommand> commands = captureCreateCommands(2);
        assertCommand(findCommandForUser(commands, HOST_ID), HOST_ID, GUEST_ID, NotificationType.ORDER_STATUS_CHANGED);
        assertCommand(findCommandForUser(commands, GUEST_ID), GUEST_ID, HOST_ID, NotificationType.REFUND_COMPLETED);
    }

    @Test
    void sendDisputeResolvedNotificationWithOnlyGuestCreatesOneCommand() {
        stubUser(GUEST_ID);

        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, null, ORDER_NUMBER, HOMESTAY_TITLE, "APPROVED", RESOLUTION_NOTE);

        List<NotificationCreateCommand> commands = captureCreateCommands(1);
        assertCommand(commands.get(0), GUEST_ID, null, NotificationType.REFUND_COMPLETED);
    }

    @Test
    void sendDisputeResolvedNotificationWithNullNoteStillCreatesCommands() {
        stubUser(GUEST_ID);
        stubUser(HOST_ID);

        notificationServiceImpl.sendDisputeResolvedNotification(
                ORDER_ID, GUEST_ID, HOST_ID, ORDER_NUMBER, HOMESTAY_TITLE, "APPROVED", null);

        List<NotificationCreateCommand> commands = captureCreateCommands(2);
        assertCommand(findCommandForUser(commands, HOST_ID), HOST_ID, GUEST_ID, NotificationType.ORDER_STATUS_CHANGED);
        assertCommand(findCommandForUser(commands, GUEST_ID), GUEST_ID, HOST_ID, NotificationType.REFUND_COMPLETED);
    }

    private void stubUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private List<NotificationCreateCommand> captureCreateCommands(int count) {
        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService, times(count)).createNotification(captor.capture());
        return captor.getAllValues();
    }

    private NotificationCreateCommand findCommandForUser(List<NotificationCreateCommand> commands, Long userId) {
        return commands.stream()
                .filter(command -> userId.equals(command.userId()))
                .findFirst()
                .orElseThrow();
    }

    private void assertCommand(
            NotificationCreateCommand command,
            Long userId,
            Long actorId,
            NotificationType type) {
        assertEquals(userId, command.userId());
        assertEquals(actorId, command.actorId());
        assertEquals(type, command.type());
        assertEquals(EntityType.ORDER, command.entityType());
        assertEquals(String.valueOf(ORDER_ID), command.entityId());
    }
}
