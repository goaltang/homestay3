package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_price_snapshot",
        indexes = {
                @Index(name = "idx_order", columnList = "order_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "quote_token", length = 128)
    private String quoteToken;

    @Column(name = "room_original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal roomOriginalAmount;

    @Column(name = "daily_price_json", nullable = false, columnDefinition = "TEXT")
    private String dailyPriceJson;

    @Column(name = "activity_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal activityDiscountAmount = BigDecimal.ZERO;

    @Column(name = "full_reduction_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fullReductionAmount = BigDecimal.ZERO;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    @Column(name = "platform_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformDiscountAmount = BigDecimal.ZERO;

    @Column(name = "host_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal hostDiscountAmount = BigDecimal.ZERO;

    @Column(name = "cleaning_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cleaningFee = BigDecimal.ZERO;

    @Column(name = "service_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "payable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal payableAmount;

    @Column(name = "host_receivable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal hostReceivableAmount;

    @Column(name = "pricing_detail_json", columnDefinition = "TEXT")
    private String pricingDetailJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
