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
     * 管理员发起退款申请（用于系统/管理员主动发起的退款）
     * @param orderId 订单ID
     * @return 发起退款后的订单DTO
     */
    OrderDTO initiateRefund(Long orderId);

    /**
     * 管理员批准退款申请
     * @param orderId 订单ID
     * @param refundNote 批准备注
     * @return 批准退款后的订单DTO
     */
    OrderDTO approveRefund(Long orderId, String refundNote);

    /**
     * 管理员拒绝退款申请
     * @param orderId 订单ID
     * @param rejectReason 拒绝原因
     * @return 拒绝退款后的订单DTO
     */
    OrderDTO rejectRefund(Long orderId, String rejectReason);

    /**
     * 管理员完成退款处理（实际退款成功后）
     * @param orderId 订单ID
     * @param refundTransactionId 退款交易号
     * @return 完成退款后的订单DTO
     */
    OrderDTO completeRefund(Long orderId, String refundTransactionId);

    /**
     * 用户申请退款
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 申请退款后的订单DTO
     */
    OrderDTO requestUserRefund(Long orderId, String reason);
}