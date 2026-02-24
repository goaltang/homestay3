package com.homestay3.homestaybackend.dto.payment;

import com.homestay3.homestaybackend.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付状态响应DTO
 */
@Data
@Builder
public class PaymentStatusResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 支付平台交易号
     */
    private String transactionId;
    
    /**
     * 支付状态
     */
    private PaymentStatus status;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 响应消息
     */
    private String message;
} 