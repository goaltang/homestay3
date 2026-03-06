package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.IOrderTimeoutService;
import com.homestay3.homestaybackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时处理服务 - 优化版
 * 处理订单在各个状态的超时情况，增加更频繁的检查和预警机制
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutService implements IOrderTimeoutService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * 超时配置（单位：小时）
     */
    private static final int PENDING_TIMEOUT_HOURS = 2; // 待确认状态超时时间
    private static final int CONFIRMED_TIMEOUT_HOURS = 2; // 已确认未支付状态超时时间
    private static final int PAYMENT_PENDING_TIMEOUT_HOURS = 2; // 支付中状态超时时间

    /**
     * 预警配置（单位：分钟）
     */
    private static final int WARNING_BEFORE_TIMEOUT_MINUTES = 30; // 超时前30分钟预警

    /**
     * 主要定时任务：处理超时订单
     * 每10分钟执行一次，提高实时性
     */
    @Override
    @Scheduled(fixedRate = 10 * 60 * 1000) // 每10分钟执行一次
    public void handleTimeoutOrders() {
        log.debug("开始处理超时订单...");

        try {
            // 处理待确认超时订单
            handlePendingTimeoutOrders();

            // 处理已确认未支付超时订单
            handleConfirmedTimeoutOrders();

            // 处理支付中超时订单
            handlePaymentPendingTimeoutOrders();

            log.debug("超时订单处理完成");
        } catch (Exception e) {
            log.error("处理超时订单时发生异常", e);
        }
    }

    /**
     * 预警定时任务：发送超时预警通知
     * 每5分钟执行一次，及时提醒用户
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 每5分钟执行一次
    @Transactional(readOnly = true)
    public void sendTimeoutWarnings() {
        log.debug("开始检查超时预警...");

        try {
            // 发送待确认订单预警
            sendPendingOrderWarnings();

            // 发送待支付订单预警
            sendPaymentWarnings();

            log.debug("超时预警检查完成");
        } catch (Exception e) {
            log.error("发送超时预警时发生异常", e);
        }
    }

    /**
     * 快速检查任务：处理即将超时的订单
     * 每分钟执行一次，处理最后几分钟的订单
     */
    @Scheduled(fixedRate = 60 * 1000) // 每分钟执行一次
    public void handleImminentTimeouts() {
        log.debug("开始处理即将超时的订单...");

        try {
            // 处理即将超时的支付订单（最后5分钟）
            handleImminentPaymentTimeouts();

            log.debug("即将超时订单处理完成");
        } catch (Exception e) {
            log.error("处理即将超时订单时发生异常", e);
        }
    }

    /**
     * 处理待确认超时订单
     */
    private void handlePendingTimeoutOrders() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(PENDING_TIMEOUT_HOURS);
        log.debug("查找创建时间早于 {} 的 PENDING 订单", timeoutThreshold);

        List<Order> timeoutOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING.name(), timeoutThreshold);

        if (!timeoutOrders.isEmpty()) {
            log.info("找到 {} 个待确认超时订单", timeoutOrders.size());

            for (Order order : timeoutOrders) {
                try {
                    orderService.systemCancelOrder(order.getId(),
                            OrderStatus.CANCELLED_SYSTEM.name(),
                            "系统自动取消：2小时内未确认");
                    log.info("已自动取消超时待确认订单: {}", order.getOrderNumber());

                    // TODO: 发送取消通知给用户
                    // notificationService.sendOrderCancelledNotification(order);

                } catch (Exception e) {
                    log.error("取消超时待确认订单失败: {}, 错误: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 处理已确认未支付超时订单
     */
    private void handleConfirmedTimeoutOrders() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(CONFIRMED_TIMEOUT_HOURS);
        log.debug("查找更新时间早于 {} 的 CONFIRMED 订单", timeoutThreshold);

        List<Order> timeoutOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                OrderStatus.CONFIRMED.name(), timeoutThreshold);

        if (!timeoutOrders.isEmpty()) {
            log.info("找到 {} 个已确认未支付超时订单", timeoutOrders.size());

            for (Order order : timeoutOrders) {
                try {
                    orderService.systemCancelOrder(order.getId(),
                            OrderStatus.CANCELLED_SYSTEM.name(),
                            "系统自动取消：2小时内未支付");
                    log.info("已自动取消超时未支付订单: {}", order.getOrderNumber());

                    // TODO: 发送取消通知给用户
                    // notificationService.sendOrderCancelledNotification(order);

                } catch (Exception e) {
                    log.error("取消超时未支付订单失败: {}, 错误: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 处理支付中超时订单
     */
    private void handlePaymentPendingTimeoutOrders() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(PAYMENT_PENDING_TIMEOUT_HOURS);
        log.debug("查找更新时间早于 {} 的 PAYMENT_PENDING 订单", timeoutThreshold);

        List<Order> timeoutOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                OrderStatus.PAYMENT_PENDING.name(), timeoutThreshold);

        if (!timeoutOrders.isEmpty()) {
            log.info("找到 {} 个支付中超时订单", timeoutOrders.size());

            for (Order order : timeoutOrders) {
                try {
                    orderService.systemCancelOrder(order.getId(),
                            OrderStatus.CANCELLED_SYSTEM.name(),
                            "系统自动取消：支付超时");
                    log.info("已自动取消支付超时订单: {}", order.getOrderNumber());

                    // TODO: 发送取消通知给用户
                    // notificationService.sendOrderCancelledNotification(order);

                } catch (Exception e) {
                    log.error("取消支付超时订单失败: {}, 错误: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 处理即将超时的支付订单（最后5分钟）
     */
    private void handleImminentPaymentTimeouts() {
        // 查找即将在5分钟内超时的支付中订单
        LocalDateTime imminentThreshold = LocalDateTime.now().minusHours(CONFIRMED_TIMEOUT_HOURS).plusMinutes(5);
        LocalDateTime currentThreshold = LocalDateTime.now().minusHours(CONFIRMED_TIMEOUT_HOURS);

        List<Order> imminentOrders = orderRepository.findByStatusAndUpdatedAtBetween(
                OrderStatus.CONFIRMED.name(), currentThreshold, imminentThreshold);

        for (Order order : imminentOrders) {
            // TODO: 发送紧急通知给用户
            // notificationService.sendUrgentPaymentReminder(order);
            log.debug("订单 {} 即将在5分钟内超时", order.getOrderNumber());
        }
    }

    /**
     * 发送待确认订单预警
     */
    private void sendPendingOrderWarnings() {
        LocalDateTime warningThreshold = LocalDateTime.now()
                .minusHours(PENDING_TIMEOUT_HOURS)
                .plusMinutes(WARNING_BEFORE_TIMEOUT_MINUTES);

        List<Order> warningOrders = orderRepository.findByStatusAndCreatedAtBetween(
                OrderStatus.PENDING.name(),
                warningThreshold.minusMinutes(5), // 避免重复发送
                warningThreshold);

        for (Order order : warningOrders) {
            // TODO: 发送预警通知
            // notificationService.sendTimeoutWarning(order, "请尽快确认订单，否则将在30分钟后自动取消");
            log.debug("发送待确认订单预警: {}", order.getOrderNumber());
        }
    }

    /**
     * 发送待支付订单预警
     */
    private void sendPaymentWarnings() {
        LocalDateTime warningThreshold = LocalDateTime.now()
                .minusHours(CONFIRMED_TIMEOUT_HOURS)
                .plusMinutes(WARNING_BEFORE_TIMEOUT_MINUTES);

        List<Order> warningOrders = orderRepository.findByStatusAndUpdatedAtBetween(
                OrderStatus.CONFIRMED.name(),
                warningThreshold.minusMinutes(5), // 避免重复发送
                warningThreshold);

        for (Order order : warningOrders) {
            // TODO: 发送预警通知
            // notificationService.sendTimeoutWarning(order, "请尽快完成支付，否则将在30分钟后自动取消");
            log.debug("发送待支付订单预警: {}", order.getOrderNumber());
        }
    }
}