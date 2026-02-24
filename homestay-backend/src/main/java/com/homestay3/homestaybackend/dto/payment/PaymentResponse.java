package com.homestay3.homestaybackend.dto.payment;

import lombok.Builder;
import lombok.Data;

/**
 * 支付响应DTO
 */
@Data
@Builder
public class PaymentResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 支付二维码URL（扫码支付用）
     */
    private String qrCode;
    
    /**
     * 支付页面URL（页面跳转支付用）
     */
    private String paymentUrl;
    
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 错误代码
     */
    private String errorCode;
} 