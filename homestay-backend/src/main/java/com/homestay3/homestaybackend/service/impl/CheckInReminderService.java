package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationCreateCommand;
import com.homestay3.homestaybackend.entity.CheckInRecord;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.notification.OrderNotificationEventType;
import com.homestay3.homestaybackend.repository.CheckInRecordRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class CheckInReminderService {

    private static final Logger log = LoggerFactory.getLogger(CheckInReminderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM月dd日");

    private final OrderRepository orderRepository;
    private final CheckInRecordRepository checkInRecordRepository;
    private final NotificationService notificationService;
    private final WebSocketNotificationService webSocketNotificationService;

    public CheckInReminderService(OrderRepository orderRepository,
                                  CheckInRecordRepository checkInRecordRepository,
                                  NotificationService notificationService,
                                  WebSocketNotificationService webSocketNotificationService) {
        this.orderRepository = orderRepository;
        this.checkInRecordRepository = checkInRecordRepository;
        this.notificationService = notificationService;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendCheckInReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<String> targetStatuses = Arrays.asList("PAID", "READY_FOR_CHECKIN", "CHECKED_IN");

        log.info("开始执行入住前提醒任务，目标日期: {}, 状态: {}", tomorrow, targetStatuses);

        List<Order> orders = orderRepository.findByStatusInAndCheckInDate(targetStatuses, tomorrow);

        log.info("找到 {} 个需要提醒的订单", orders.size());

        for (Order order : orders) {
            try {
                sendReminderToGuest(order);
                sendReminderToHost(order);
            } catch (Exception e) {
                log.error("发送入住提醒失败, orderId={}, error={}", order.getId(), e.getMessage(), e);
            }
        }

        log.info("入住前提醒任务执行完成");
    }

    private void sendReminderToGuest(Order order) {
        Long guestId = order.getGuest().getId();
        String homestayTitle = order.getHomestay().getTitle();
        String checkInDate = order.getCheckInDate().format(DATE_FORMATTER);

        String checkInCode = getCheckInCode(order.getId());

        String content;
        if (checkInCode != null && !checkInCode.isEmpty()) {
            content = String.format("您明天（%s）即将入住 [%s]，请查看入住凭证并按时入住。入住码：%s",
                    checkInDate, homestayTitle, checkInCode);
        } else {
            content = String.format("您明天（%s）即将入住 [%s]，请查看入住凭证并按时入住。",
                    checkInDate, homestayTitle);
        }

        notificationService.createNotification(NotificationCreateCommand.orderEvent(
                guestId,
                null,
                OrderNotificationEventType.BOOKING_REMINDER,
                order.getId(),
                content
        ));

        long unreadCount = notificationService.getUnreadNotificationCount(guestId);
        webSocketNotificationService.sendUnreadCountToUser(guestId, unreadCount);

        log.info("已发送入住提醒给客人: guestId={}, orderId={}", guestId, order.getId());
    }

    private void sendReminderToHost(Order order) {
        Long hostId = order.getHomestay().getOwner().getId();
        String homestayTitle = order.getHomestay().getTitle();

        String content = String.format("您的房源 [%s] 明天有房客入住，请确认房源已准备好。订单号：%s",
                homestayTitle, order.getOrderNumber());

        notificationService.createNotification(NotificationCreateCommand.orderEvent(
                hostId,
                null,
                OrderNotificationEventType.BOOKING_REMINDER,
                order.getId(),
                content
        ));

        long unreadCount = notificationService.getUnreadNotificationCount(hostId);
        webSocketNotificationService.sendUnreadCountToUser(hostId, unreadCount);

        log.info("已发送入住提醒给房东: hostId={}, orderId={}", hostId, order.getId());
    }

    private String getCheckInCode(Long orderId) {
        return checkInRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .map(CheckInRecord::getCheckInCode)
                .filter(code -> code != null && !code.isEmpty())
                .orElse(null);
    }
}
