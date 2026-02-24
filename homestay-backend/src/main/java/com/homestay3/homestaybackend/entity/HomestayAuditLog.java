package com.homestay3.homestaybackend.entity;

import com.homestay3.homestaybackend.model.HomestayStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 房源审核记录实体
 * 记录房源审核的完整历史轨迹
 */
@Entity
@Table(name = "homestay_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的房源ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homestay_id", nullable = false)
    private Homestay homestay;

    /**
     * 审核员
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    /**
     * 变更前状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private HomestayStatus oldStatus;

    /**
     * 变更后状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private HomestayStatus newStatus;

    /**
     * 审核操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private AuditActionType actionType;

    /**
     * 审核原因/拒绝原因
     */
    @Column(name = "review_reason", columnDefinition = "TEXT")
    private String reviewReason;

    /**
     * 审核员备注
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /**
     * 审核时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 客户端IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 用户代理信息
     */
    @Column(name = "user_agent")
    private String userAgent;

    /**
     * 审核操作类型枚举
     */
    public enum AuditActionType {
        SUBMIT("提交审核"),
        APPROVE("批准"),
        REJECT("拒绝"),
        RESUBMIT("重新提交"),
        WITHDRAW("撤回审核"),

        ACTIVATE("上架"),
        DEACTIVATE("下架"),
        SUSPEND("暂停"),
        DELETE("删除");

        private final String description;

        AuditActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
