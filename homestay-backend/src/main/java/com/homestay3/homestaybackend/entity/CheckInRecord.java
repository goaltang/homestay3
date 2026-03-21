package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 入住记录实体
 * 记录客人入住办理的完整审计轨迹
 */
@Entity
@Table(name = "check_in_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 入住办理方式: SELF_SERVICE(自助入住), MANUAL(前台办理), CODE(入住码)
     */
    @Column(name = "check_in_method", length = 30)
    private String checkInMethod;

    /**
     * 实际入住时间
     */
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    /**
     * 入住办理者ID
     */
    @Column(name = "check_in_operator_id")
    private Long checkInOperatorId;

    /**
     * 入住办理者类型: HOST(房东), ADMIN(管理员), SYSTEM(系统自动), SELF_SERVICE(自助)
     */
    @Column(name = "check_in_operator_type", length = 20)
    private String checkInOperatorType;

    /**
     * 入住凭证 - 入住码
     */
    @Column(name = "check_in_code", length = 32)
    private String checkInCode;

    /**
     * 入住凭证 - 门锁密码
     */
    @Column(name = "door_password", length = 32)
    private String doorPassword;

    /**
     * 入住凭证 - 密钥箱密码
     */
    @Column(name = "lockbox_code", length = 32)
    private String lockboxCode;

    /**
     * 入住凭证 - 物理位置描述
     */
    @Column(name = "location_description", length = 200)
    private String locationDescription;

    /**
     * 入住凭证有效期开始
     */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /**
     * 入住凭证有效期结束
     */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /**
     * 入住备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    /**
     * 状态: ACTIVE(有效), EXPIRED(已过期), CANCELLED(已取消)
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
