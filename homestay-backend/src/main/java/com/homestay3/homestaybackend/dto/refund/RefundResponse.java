package com.homestay3.homestaybackend.dto.refund;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundResponse {
    
    private boolean success;
    
    private String outTradeNo;
    
    private String refundTradeNo;
    
    private BigDecimal refundAmount;
    
    private String message;
    
    private String errorCode;
    
    private String errorMessage;
}
