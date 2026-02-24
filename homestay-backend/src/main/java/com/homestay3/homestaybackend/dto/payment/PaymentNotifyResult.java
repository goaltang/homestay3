package com.homestay3.homestaybackend.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付回调结果DTO
 */
@Data
@Builder
public class PaymentNotifyResult {
    
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
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 回调参数
     */
    private Map<String, String> params;
    
    /**
     * 消息
     */
    private String message;
} 