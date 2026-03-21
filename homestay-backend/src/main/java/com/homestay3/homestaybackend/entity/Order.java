package com.homestay3.homestaybackend.entity;

import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.RefundType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", 
       indexes = {
           @Index(name = "idx_homestay_dates", columnList = "homestay_id, check_in_date, check_out_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homestay_id", nullable = false)
    private Homestay homestay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private Integer nights;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String remark;

    // 退款相关字段
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type")
    private RefundType refundType;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_initiated_by")
    private Long refundInitiatedBy; // 退款发起者用户ID

    @Column(name = "refund_initiated_at")
    private LocalDateTime refundInitiatedAt;

    @Column(name = "refund_processed_by")
    private Long refundProcessedBy; // 退款处理者用户ID

    @Column(name = "refund_processed_at")
    private LocalDateTime refundProcessedAt;

    @Column(name = "refund_transaction_id")
    private String refundTransactionId;

    @Column(name = "refund_rejection_reason", length = 500)
    private String refundRejectionReason; // 退款被拒绝时的原因

    // 争议相关字段
    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason; // 争议原因

    @Column(name = "dispute_raised_by")
    private Long disputeRaisedBy; // 争议发起人用户ID

    @Column(name = "dispute_raised_at")
    private LocalDateTime disputeRaisedAt; // 争议发起时间

    @Column(name = "dispute_resolved_at")
    private LocalDateTime disputeResolvedAt; // 争议解决时间

    @Column(name = "dispute_resolution", length = 20)
    private String disputeResolution; // 仲裁结果：APPROVED(批准退款)/REJECTED(拒绝退款)

    @Column(name = "dispute_resolution_note", length = 500)
    private String disputeResolutionNote; // 仲裁备注

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    // 入住相关字段
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt; // 实际入住时间

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt; // 实际退房时间

    @Column(name = "check_in_code", length = 32)
    private String checkInCode; // 入住码

    @Column(name = "door_password", length = 32)
    private String doorPassword; // 门锁密码

    @Column(name = "auto_checkin_time", length = 5)
    private String autoCheckinTime; // 自动入住时间

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount; // 押金金额

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
