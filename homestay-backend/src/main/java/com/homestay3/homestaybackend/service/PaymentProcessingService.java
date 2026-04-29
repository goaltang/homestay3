package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.dto.refund.RefundRequest;
import com.homestay3.homestaybackend.dto.refund.RefundResponse;

/**
 * 支付处理服务
 * 封装所有与订单支付和退款相关的业务逻辑
 */
public interface PaymentProcessingService {

    /**
     * 处理订单支付（使用默认支付方式）
     * @param orderId 订单ID
     * @return 支付后的订单DTO
     */
    OrderDTO processPayment(Long orderId);

    /**
     * 处理订单支付（使用指定支付方式）
     * @param orderId 订单ID
     * @param paymentMethod 支付方式
     * @return 支付后的订单DTO
     */
    OrderDTO processPayment(Long orderId, String paymentMethod);

    /**
     * 管理员手动确认订单支付
     * @param orderId 订单ID
     * @return 确认支付后的订单DTO
     */
    OrderDTO confirmPayment(Long orderId);

    /**
     * 管理员直接执行退款（ADMIN_INITIATED类型，不需要审批流程）
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 执行退款后的订单DTO
     */
    OrderDTO executeRefund(Long orderId, String reason);

    /**
     * 审批并执行退款（统一入口：USER_REQUESTED由房东审批，HOST_CANCELLED由管理员审批）
     * @param orderId 订单ID
     * @param refundNote 批准备注
     * @return 批准退款后的订单DTO
     */
    OrderDTO approveRefund(Long orderId, String refundNote);

    /**
     * 拒绝退款申请
     * @param orderId 订单ID
     * @param rejectReason 拒绝原因
     * @return 拒绝退款后的订单DTO
     */
    OrderDTO rejectRefund(Long orderId, String rejectReason);

    /**
     * 用户申请退款（USER_REQUESTED类型）
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 申请退款后的订单DTO
     */
    OrderDTO requestUserRefund(Long orderId, String reason);

    /**
     * 统一订单支付成功后置处理（核销优惠券、更新活动流水、生成收益、发送通知等）
     * 所有支付成功路径（真实支付回调、查单成功、mock成功、手动确认）必须调用此统一入口
     * @param orderId 已支付订单ID
     */
    void handleOrderPaidSuccess(Long orderId);
}