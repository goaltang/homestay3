package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupon",
        indexes = {
                @Index(name = "idx_user_status", columnList = "user_id, status, expire_at"),
                @Index(name = "idx_locked_order", columnList = "locked_order_id"),
                @Index(name = "idx_used_order", columnList = "used_order_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private CouponTemplate template;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_code", nullable = false, unique = true, length = 64)
    private String couponCode;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "locked_order_id")
    private Long lockedOrderId;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    // --- 领取快照字段（防止后台修改模板后追溯影响已发放券）---
    @Column(name = "snapshot_face_value", precision = 12, scale = 2)
    private java.math.BigDecimal snapshotFaceValue;

    @Column(name = "snapshot_discount_rate", precision = 3, scale = 2)
    private java.math.BigDecimal snapshotDiscountRate;

    @Column(name = "snapshot_threshold_amount", precision = 12, scale = 2)
    private java.math.BigDecimal snapshotThresholdAmount;

    @Column(name = "snapshot_max_discount", precision = 12, scale = 2)
    private java.math.BigDecimal snapshotMaxDiscount;

    @Column(name = "snapshot_scope_type", length = 50)
    private String snapshotScopeType;

    @Column(name = "snapshot_scope_value_json", columnDefinition = "TEXT")
    private String snapshotScopeValueJson;

    @Column(name = "snapshot_subsidy_bearer", length = 50)
    private String snapshotSubsidyBearer;
    // --------------------------------------------------------

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
