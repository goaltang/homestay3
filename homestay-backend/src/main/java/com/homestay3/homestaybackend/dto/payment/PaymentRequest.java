package com.homestay3.homestaybackend.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求DTO
 */
@Data
@Builder
public class PaymentRequest {
    
    /**
     * 商户订单号
     */
    private String outTradeNo;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 商品标题
     */
    private String subject;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 商品描述
     */
    private String body;
} 