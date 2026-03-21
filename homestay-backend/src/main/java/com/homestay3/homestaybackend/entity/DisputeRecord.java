package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 争议记录实体
 * 记录每次争议的完整历史
 */
@Entity
@Table(name = "dispute_record", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_raised_by", columnList = "raised_by"),
    @Index(name = "idx_resolved_by", columnList = "resolved_by")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的订单ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 争议原因
     */
    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    /**
     * 发起争议的用户ID
     */
    @Column(name = "raised_by")
    private Long raisedBy;

    /**
     * 发起时间
     */
    @Column(name = "raised_at")
    private LocalDateTime raisedAt;

    /**
     * 仲裁结果：APPROVED（批准退款）/ REJECTED（拒绝退款）
     */
    @Column(name = "resolution", length = 20)
    private String resolution;

    /**
     * 仲裁人ID
     */
    @Column(name = "resolved_by")
    private Long resolvedBy;

    /**
     * 仲裁时间
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * 仲裁备注
     */
    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
