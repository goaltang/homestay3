package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_template",
        indexes = {
                @Index(name = "idx_status_time", columnList = "status, valid_start_at, valid_end_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "coupon_type", nullable = false, length = 50)
    private String couponType;

    @Column(name = "face_value", precision = 12, scale = 2)
    private BigDecimal faceValue;

    @Column(name = "discount_rate", precision = 3, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "threshold_amount", precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "total_stock", nullable = false)
    @Builder.Default
    private Integer totalStock = 0;

    @Column(name = "issued_count", nullable = false)
    @Builder.Default
    private Integer issuedCount = 0;

    @Column(name = "per_user_limit", nullable = false)
    @Builder.Default
    private Integer perUserLimit = 1;

    @Column(name = "valid_type", nullable = false, length = 50)
    private String validType;

    @Column(name = "valid_days")
    private Integer validDays;

    @Column(name = "valid_start_at")
    private LocalDateTime validStartAt;

    @Column(name = "valid_end_at")
    private LocalDateTime validEndAt;

    @Column(name = "scope_type", nullable = false, length = 50)
    @Builder.Default
    private String scopeType = "ALL";

    @Column(name = "scope_value_json", columnDefinition = "TEXT")
    private String scopeValueJson;

    @Column(name = "subsidy_bearer", nullable = false, length = 50)
    @Builder.Default
    private String subsidyBearer = "PLATFORM";

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "is_new_user_coupon", nullable = false)
    @Builder.Default
    private Boolean isNewUserCoupon = false;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT";

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
