package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 违规举报实体
 */
@Entity
@Table(name = "violation_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 被举报的房源
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homestay_id", nullable = false)
    private Homestay homestay;

    /**
     * 举报人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * 违规类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType violationType;

    /**
     * 举报原因
     */
    @Column(nullable = false, length = 1000)
    private String reason;

    /**
     * 详细描述
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * 证据图片URL（多个URL用逗号分隔）
     */
    @Column(columnDefinition = "TEXT")
    private String evidenceImages;

    /**
     * 举报状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * 处理结果
     */
    @Column(length = 1000)
    private String processResult;

    /**
     * 处理人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 违规类型枚举
     */
    public enum ViolationType {
        PRICE_FRAUD("价格欺诈"),
        CONTENT_VIOLATION("内容违规"),
        DESCRIPTION_VIOLATION("描述不实"),
        IMAGE_VIOLATION("图片违规"),
        IDENTITY_FRAUD("身份造假"),
        SERVICE_VIOLATION("服务违规"),
        SAFETY_VIOLATION("安全违规"),
        OTHER("其他");

        private final String description;

        ViolationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 举报状态枚举
     */
    public enum ReportStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        VERIFIED("已核实"),
        DISMISSED("已忽略"),
        RESOLVED("已解决");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
