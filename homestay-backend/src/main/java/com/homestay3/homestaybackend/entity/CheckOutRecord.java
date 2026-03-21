package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退房记录实体
 * 记录客人退房结算的完整审计轨迹
 */
@Entity
@Table(name = "check_out_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 退房办理方式: SELF_SERVICE(自助退房), MANUAL(前台办理), AUTO(自动退房)
     */
    @Column(name = "check_out_method", length = 30)
    private String checkOutMethod;

    /**
     * 实际退房时间
     */
    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    /**
     * 退房办理者ID
     */
    @Column(name = "check_out_operator_id")
    private Long checkOutOperatorId;

    /**
     * 退房办理者类型: HOST(房东), GUEST(客人), ADMIN(管理员), SYSTEM(系统自动)
     */
    @Column(name = "check_out_operator_type", length = 20)
    private String checkOutOperatorType;

    /**
     * 实际住宿天数
     */
    @Column(name = "actual_nights")
    private Integer actualNights;

    /**
     * 押金金额
     */
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    /**
     * 押金状态: PENDING(待处理), PAID(已付), REFUNDED(已退), RETAINED(扣押), WAIVED(免除)
     */
    @Column(name = "deposit_status", length = 20)
    private String depositStatus;

    /**
     * 额外费用 (损坏赔偿等)
     */
    @Column(name = "extra_charges", precision = 10, scale = 2)
    private BigDecimal extraCharges;

    /**
     * 额外费用说明
     */
    @Column(name = "extra_charges_description", columnDefinition = "TEXT")
    private String extraChargesDescription;

    /**
     * 结算金额 (应付 - 押金退还)
     */
    @Column(name = "settlement_amount", precision = 10, scale = 2)
    private BigDecimal settlementAmount;

    /**
     * 退房备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    /**
     * 状态: PENDING(待结算), COMPLETED(已结算), DISPUTED(有争议)
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
