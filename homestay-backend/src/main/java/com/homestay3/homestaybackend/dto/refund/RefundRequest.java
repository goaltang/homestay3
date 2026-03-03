package com.homestay3.homestaybackend.dto.refund;

import com.homestay3.homestaybackend.model.RefundType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundRequest {
    
    private Long orderId;
    
    private String outTradeNo;
    
    private BigDecimal refundAmount;
    
    private String refundReason;
    
    private RefundType refundType;
    
    private String paymentMethod;
}
