package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.RefundType;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.EarningService;
import com.homestay3.homestaybackend.service.OrderNotificationService;
import com.homestay3.homestaybackend.service.PaymentProcessingService;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 支付处理服务实现
 * 封装所有与订单支付相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingServiceImpl.class);
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EarningService earningService;
    private final OrderNotificationService orderNotificationService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    @Transactional
    public OrderDTO processPayment(Long orderId) {
        // 使用默认支付方式
        return processPayment(orderId, "default");
    }

    @Override
    @Transactional
    public OrderDTO processPayment(Long orderId, String paymentMethod) {
        log.info("开始处理订单 ID: {} 的支付请求，支付方式: {}", orderId, paymentMethod);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        log.debug("找到订单: {}, 当前状态: {}", order.getOrderNumber(), order.getStatus());

        // 检查当前用户是否有权限
        User currentUser = getCurrentUser();
        log.debug("执行支付操作的用户: {}", currentUser.getUsername());
        if (!isOrderGuest(order, currentUser)) {
            log.warn("用户 {} 尝试支付不属于自己的订单 {}，操作被拒绝。", currentUser.getUsername(), order.getOrderNumber());
            throw new IllegalArgumentException("只有订单的客户可以支付订单");
        }

        // 检查订单状态
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());
        log.debug("检查订单 {} 的状态是否允许支付 (当前: {})", order.getOrderNumber(), currentStatus);
        if (currentStatus != OrderStatus.CONFIRMED && currentStatus != OrderStatus.PAYMENT_PENDING) {
            log.warn("订单 {} 状态为 {}，不允许支付。", order.getOrderNumber(), currentStatus);
            throw new IllegalArgumentException("只有已确认或正在支付中的订单可以支付");
        }

        // 模拟支付处理
        log.debug("开始模拟订单 {} 的支付处理...", order.getOrderNumber());
        try {
            // 模拟支付处理延迟
            Thread.sleep(500);
            log.debug("模拟支付处理完成，准备更新订单 {} 状态为 PAID。", order.getOrderNumber());

            // 支付成功，更新订单状态
            order.setStatus(OrderStatus.PAID.name());
            order.setPaymentStatus(PaymentStatus.PAID);

            // 添加支付备注
            String remark = (order.getRemark() != null && !order.getRemark().isEmpty()) ? order.getRemark() + "\n" : "";
            remark += "支付方式: " + paymentMethod;
            order.setRemark(remark);
            log.debug("更新订单 {} 备注: {}", order.getOrderNumber(), remark);

            Order paidOrder = orderRepository.save(order);
            log.info("订单 {} 状态已成功更新为 PAID 并保存。", paidOrder.getOrderNumber());

            // --- 发送支付成功通知给房东和房客 ---
            try {
                User host = paidOrder.getHomestay().getOwner();
                User guest = paidOrder.getGuest();
                orderNotificationService.sendOrderPaymentSuccessNotification(
                        host.getId(), guest.getId(), paidOrder.getId(),
                        host.getUsername(), guest.getUsername(),
                        paidOrder.getHomestay().getTitle(),
                        paidOrder.getOrderNumber(),
                        paymentMethod);
            } catch (Exception notifyEx) {
                log.error("发送订单 {} 支付成功通知失败: {}", paidOrder.getOrderNumber(), notifyEx.getMessage(), notifyEx);
            }
            // --- 通知发送结束 ---

            // --- 添加：自动生成待结算收益记录 ---
            log.info("订单 {} 支付成功，准备调用 EarningService 生成收益记录...", paidOrder.getOrderNumber());
            EarningDTO generatedEarning = null;
            try {
                // 调用重命名后的方法
                generatedEarning = earningService.generatePendingEarningForOrder(paidOrder.getId());
                if (generatedEarning != null) {
                    log.info("为订单 {} 成功生成或获取收益记录 ID: {}", paidOrder.getOrderNumber(), generatedEarning.getId());
                } else {
                    log.warn("订单 {} 的收益记录生成调用返回为空，可能已存在或生成失败（详见 EarningService 日志）。", paidOrder.getOrderNumber());
                }
            } catch (Exception earningEx) {
                // 记录收益生成失败的错误，但不应中断支付成功流程
                log.error("为订单 {} 调用 generatePendingEarningForOrder 时发生异常: {}", paidOrder.getOrderNumber(),
                        earningEx.getMessage(), earningEx);
                // 这里可以选择是否需要额外的错误处理，比如标记订单需要人工处理收益等
            }
            log.info("EarningService 调用结束，订单 {} 的支付流程完成。", paidOrder.getOrderNumber());
            // --- 收益生成结束 ---

            return convertToDTO(paidOrder);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单 {} 支付处理被中断", order.getOrderNumber(), e);
            throw new RuntimeException("支付处理中断");
        } catch (Exception e) {
            // 支付失败，更新订单状态为支付失败
            log.error("订单 {} 支付过程中发生意外错误，尝试将状态更新为 PAYMENT_FAILED: {}", order.getOrderNumber(), e.getMessage(), e);
            order.setStatus(OrderStatus.PAYMENT_FAILED.name());
            order.setPaymentStatus(PaymentStatus.UNPAID);
            orderRepository.save(order);
            // 支付失败不应该生成收益，也不需要发通知
            throw new RuntimeException("支付失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderDTO confirmPayment(Long orderId) {
        log.info("管理员手动确认订单支付，ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));

        // 简单校验：只有未支付的订单才能被手动确认支付
        if (order.getPaymentStatus() != PaymentStatus.UNPAID) {
            log.warn("订单 {} 的支付状态为 {}，不能手动确认支付", orderId, order.getPaymentStatus());
            throw new IllegalStateException("只有未支付的订单才能被确认支付");
        }

        // 更新状态
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        // 可选：记录支付方式为手动确认
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isEmpty()) {
            order.setPaymentMethod("MANUAL_CONFIRM");
        }
        // 可选：更新支付时间
        // order.setPaymentTime(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        log.info("订单 {} 已手动确认支付", orderId);

        // 触发支付成功后的逻辑（如生成收益）
        earningService.generatePendingEarningForOrder(updatedOrder.getId());

        return convertToDTO(updatedOrder);
    }

    // 辅助方法：获取当前登录用户
    private User getCurrentUser() {
        return userRepository.findByUsername(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    // 辅助方法：检查用户是否为订单的客户
    private boolean isOrderGuest(Order order, User user) {
        return order.getGuest() != null && order.getGuest().getId().equals(user.getId());
    }

    // 辅助方法：将 Order 实体转换为 OrderDTO
    private OrderDTO convertToDTO(Order order) {
        if (order == null) {
            return null;
        }

        // 检查订单是否已被评价
        boolean isReviewed = false; // 这里需要实际的reviewRepository，但为了简化先设为false
        // In a real implementation, you would inject ReviewRepository and check:
        // boolean isReviewed = reviewRepository.existsByOrder(order);

        // 获取房东信息
        User host = order.getHomestay() != null ? order.getHomestay().getOwner() : null;
        String hostName = host != null ? (host.getNickname() != null ? host.getNickname() : host.getUsername()) : null;
        Long hostId = host != null ? host.getId() : null;

        // 获取房客信息
        User guest = order.getGuest();
        String guestName = guest != null ? (guest.getNickname() != null ? guest.getNickname() : guest.getUsername()) : null;
        Long guestId = guest != null ? guest.getId() : null;

        // 计算明细费用
        BigDecimal baseAmount = order.getPrice() != null ? order.getPrice().multiply(BigDecimal.valueOf(order.getNights())) : BigDecimal.ZERO;
        BigDecimal cleaningFee = order.getPrice() != null ? order.getPrice().multiply(BigDecimal.valueOf(0.1)) : BigDecimal.ZERO;
        BigDecimal serviceFee = baseAmount.multiply(BigDecimal.valueOf(0.15));

        // 构建 OrderDTO
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .homestayId(order.getHomestay() != null ? order.getHomestay().getId() : null)
                .homestayTitle(order.getHomestay() != null ? order.getHomestay().getTitle() : null)
                .guestId(guestId)
                .guestName(guestName)
                .guestPhone(order.getGuestPhone())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .guestCount(order.getGuestCount())
                .price(order.getPrice())
                .cleaningFee(cleaningFee)
                .serviceFee(serviceFee)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentMethod(order.getPaymentMethod())
                .remark(order.getRemark())
                .hostId(hostId)
                .hostName(hostName)
                .createTime(order.getCreatedAt())
                .updateTime(order.getUpdatedAt())
                .isReviewed(isReviewed)
                .build();
    }
}