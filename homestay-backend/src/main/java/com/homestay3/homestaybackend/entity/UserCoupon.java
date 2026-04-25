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
