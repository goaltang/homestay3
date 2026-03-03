package com.homestay3.homestaybackend.service.gateway;

import com.homestay3.homestaybackend.dto.payment.*;
import com.homestay3.homestaybackend.dto.refund.RefundRequest;
import com.homestay3.homestaybackend.dto.refund.RefundResponse;

import java.util.Map;

/**
 * 支付网关接口
 */
public interface PaymentGateway {
    
    /**
     * 创建支付订单
     */
    PaymentResponse createPayment(PaymentRequest request);
    
    /**
     * 查询支付状态
     */
    PaymentStatusResponse queryPayment(String outTradeNo);
    
    /**
     * 验证回调签名
     */
    boolean verifyNotify(Map<String, String> params);
    
/**
     * 处理支付回调
     */
    PaymentNotifyResult handleNotify(Map<String, String> params);
    
    /**
     * 处理退款
     */
    RefundResponse processRefund(RefundRequest request);
}