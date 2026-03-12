package com.homestay3.homestaybackend.service;

/**
 * 支付处理服务
 * 封装所有与订单支付相关的业务逻辑
 */
public interface PaymentProcessingService {

    /**
     * 处理订单支付（使用默认支付方式）
     * @param orderId 订单ID
     * @return 支付后的订单DTO
     */
    com.homestay3.homestaybackend.dto.OrderDTO processPayment(Long orderId);

    /**
     * 处理订单支付（使用指定支付方式）
     * @param orderId 订单ID
     * @param paymentMethod 支付方式
     * @return 支付后的订单DTO
     */
    com.homestay3.homestaybackend.dto.OrderDTO processPayment(Long orderId, String paymentMethod);

    /**
     * 管理员手动确认订单支付
     * @param orderId 订单ID
     * @return 确认支付后的订单DTO
     */
    com.homestay3.homestaybackend.dto.OrderDTO confirmPayment(Long orderId);
}