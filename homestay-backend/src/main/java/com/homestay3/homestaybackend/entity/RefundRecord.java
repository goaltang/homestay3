package com.homestay3.homestaybackend.entity;

import com.homestay3.homestaybackend.model.RefundStatus;
import com.homestay3.homestaybackend.model.RefundType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;
    
    @Column(name = "out_trade_no", length = 64)
    private String outTradeNo;
    
    @Column(name = "trade_no", length = 64)
    private String tradeNo;
    
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", length = 50)
    private RefundType refundType;
    
    @Column(name = "out_request_no", length = 64)
    private String outRequestNo;
    
    @Column(name = "refund_trade_no", length = 64)
    private String refundTradeNo;
    
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    @Column(name = "response_params", columnDefinition = "TEXT")
    private String responseParams;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
