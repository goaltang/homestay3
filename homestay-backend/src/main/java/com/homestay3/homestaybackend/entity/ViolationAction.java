package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 违规处理记录实体
 */
@Entity
@Table(name = "violation_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 被处理的房源
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homestay_id", nullable = false)
    private Homestay homestay;

    /**
     * 关联的举报记录
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_report_id")
    private ViolationReport violationReport;

    /**
     * 处理类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    /**
     * 处理原因
     */
    @Column(nullable = false, length = 1000)
    private String reason;

    /**
     * 详细说明
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * 暂停天数（仅当actionType为SUSPEND时有效）
     */
    private Integer suspendDays;

    /**
     * 处理人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id", nullable = false)
    private User processor;

    /**
     * 处理时间
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveAt;

    /**
     * 到期时间（针对暂停操作）
     */
    private LocalDateTime expiresAt;

    /**
     * 处理类型枚举
     */
    public enum ActionType {
        WARNING("警告"),
        SUSPEND("暂停"),
        BAN("永久封禁"),
        DISMISS("忽略举报"),
        MODIFY_REQUIRED("要求修改");

        private final String description;

        ActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
