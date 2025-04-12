package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.OrderDTO;

/**
 * 支付服务接口
 */
public interface PaymentService {
    /**
     * 生成支付二维码
     * @param orderId 订单ID
     * @param method 支付方式（wechat或alipay）
     * @return 支付二维码URL
     */
    String generatePaymentQRCode(Long orderId, String method);

    /**
     * 检查支付状态
     * @param orderId 订单ID
     * @return 是否已支付
     */
    boolean checkPaymentStatus(Long orderId);

    /**
     * 模拟支付成功（仅用于测试）
     * @param orderId 订单ID
     * @return 更新后的订单信息
     */
    OrderDTO mockSuccessPayment(Long orderId);
} 