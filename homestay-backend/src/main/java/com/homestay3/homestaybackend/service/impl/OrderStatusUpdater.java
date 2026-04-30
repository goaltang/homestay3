package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单状态统一更新器
 * 
 * 职责：统一维护 {@code Order.status} 与 {@code Order.paymentStatus} 的同步关系。
 * 禁止任何 Service 直接调用 {@code order.setStatus()} / {@code order.setPaymentStatus()}，
 * 所有状态变更必须通过本类完成，避免双状态字段不一致。
 * 
 * 所有 Order 实体的状态变更已统一迁移到本类，禁止在其他 Service 中直接调用 setStatus/setPaymentStatus。
 */
@Component
@Slf4j
public class OrderStatusUpdater {

    /**
     * 支付成功
     */
    public void markPaid(Order order) {
        update(order, OrderStatus.PAID, PaymentStatus.PAID, "支付成功");
    }

    /**
     * 支付失败
     */
    public void markPaymentFailed(Order order) {
        update(order, OrderStatus.PAYMENT_FAILED, PaymentStatus.UNPAID, "支付失败");
    }

    /**
     * 管理员手动确认支付（无需真实退款）
     */
    public void markManualConfirmedPaid(Order order) {
        update(order, OrderStatus.PAID, PaymentStatus.PAID, "手动确认支付");
    }

    /**
     * 用户申请退款
     */
    public void markRefundPending(Order order) {
        update(order, OrderStatus.REFUND_PENDING, PaymentStatus.REFUND_PENDING, "用户申请退款");
    }

    /**
     * 退款完成（已退款）
     */
    public void markRefunded(Order order) {
        update(order, OrderStatus.REFUNDED, PaymentStatus.REFUNDED, "退款完成");
    }

    /**
     * 退款被拒绝，恢复到已支付状态
     */
    public void markRefundRejected(Order order) {
        update(order, OrderStatus.PAID, PaymentStatus.PAID, "退款被拒绝");
    }

    /**
     * 创建订单（待确认）
     */
    public void markPending(Order order) {
        update(order, OrderStatus.PENDING, PaymentStatus.UNPAID, "创建订单-待确认");
    }

    /**
     * 创建订单（即时预订，已确认待支付）
     */
    public void markConfirmed(Order order) {
        update(order, OrderStatus.CONFIRMED, PaymentStatus.UNPAID, "创建订单-已确认");
    }

    /**
     * 房东确认订单
     */
    public void markHostConfirmed(Order order) {
        update(order, OrderStatus.CONFIRMED, PaymentStatus.UNPAID, "房东确认订单");
    }

    /**
     * 房东拒绝订单
     */
    public void markRejected(Order order) {
        update(order, OrderStatus.REJECTED, PaymentStatus.UNPAID, "房东拒绝订单");
    }

    /**
     * 订单取消（通用）
     */
    public void markCancelled(Order order) {
        update(order, OrderStatus.CANCELLED, PaymentStatus.UNPAID, "订单已取消");
    }

    /**
     * 用户取消订单
     */
    public void markCancelledByUser(Order order) {
        update(order, OrderStatus.CANCELLED_BY_USER, PaymentStatus.UNPAID, "用户取消订单");
    }

    /**
     * 房东取消订单
     */
    public void markCancelledByHost(Order order) {
        update(order, OrderStatus.CANCELLED_BY_HOST, PaymentStatus.UNPAID, "房东取消订单");
    }

    /**
     * 系统取消订单
     */
    public void markCancelledBySystem(Order order) {
        update(order, OrderStatus.CANCELLED_SYSTEM, PaymentStatus.UNPAID, "系统取消订单");
    }

    /**
     * 办理入住
     */
    public void markCheckedIn(Order order) {
        update(order, OrderStatus.CHECKED_IN, PaymentStatus.PAID, "已入住");
    }

    /**
     * 办理退房
     */
    public void markCheckedOut(Order order) {
        update(order, OrderStatus.CHECKED_OUT, PaymentStatus.PAID, "已退房");
    }

    /**
     * 订单完成
     */
    public void markCompleted(Order order) {
        update(order, OrderStatus.COMPLETED, PaymentStatus.PAID, "订单已完成");
    }

    /**
     * 准备入住
     */
    public void markReadyForCheckIn(Order order) {
        update(order, OrderStatus.READY_FOR_CHECKIN, PaymentStatus.PAID, "准备入住");
    }

    /**
     * 进入争议待处理状态
     */
    public void markDisputePending(Order order, String reason) {
        update(order, OrderStatus.DISPUTE_PENDING, PaymentStatus.DISPUTED, reason != null ? reason : "争议待处理");
    }

    /**
     * 进入支付中状态
     */
    public void markPaymentPending(Order order) {
        update(order, OrderStatus.PAYMENT_PENDING, PaymentStatus.UNPAID, "支付中");
    }

    /**
     * 支付成功（支持自定义原因）
     */
    public void markPaid(Order order, String reason) {
        update(order, OrderStatus.PAID, PaymentStatus.PAID, reason != null ? reason : "支付成功");
    }

    /**
     * 通用状态更新（兜底方法）
     * 
     * @param order 订单实体
     * @param status 目标订单状态
     * @param paymentStatus 目标支付状态
     * @param reason 变更原因（用于日志）
     */
    public void update(Order order, OrderStatus status, PaymentStatus paymentStatus, String reason) {
        if (order == null) {
            log.warn("OrderStatusUpdater 收到空订单，跳过更新");
            return;
        }
        String oldStatus = order.getStatus();
        String oldPaymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "null";

        order.setStatus(status.name());
        order.setPaymentStatus(paymentStatus);

        log.info("订单 {} 状态更新: [{} / {}] -> [{} / {}], 原因: {}",
                order.getOrderNumber(),
                oldStatus, oldPaymentStatus,
                status.name(), paymentStatus.name(),
                reason);
    }
}
