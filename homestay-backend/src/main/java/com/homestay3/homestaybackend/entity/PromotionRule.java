package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_rule",
        indexes = {
                @Index(name = "idx_campaign", columnList = "campaign_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PromotionCampaign campaign;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "threshold_amount", precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "discount_rate", precision = 3, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_nights")
    private Integer minNights;

    @Column(name = "max_nights")
    private Integer maxNights;

    @Column(name = "scope_type", nullable = false, length = 50)
    @Builder.Default
    private String scopeType = "ALL";

    @Column(name = "scope_value_json", columnDefinition = "TEXT")
    private String scopeValueJson;

    @Column(name = "first_order_only", nullable = false)
    @Builder.Default
    private Boolean firstOrderOnly = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
