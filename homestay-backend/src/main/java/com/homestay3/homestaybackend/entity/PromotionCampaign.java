package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotion_campaign",
        indexes = {
                @Index(name = "idx_status_time", columnList = "status, start_at, end_at"),
                @Index(name = "idx_priority", columnList = "priority")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "campaign_type", nullable = false, length = 50)
    private String campaignType;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean stackable = false;

    @Column(name = "budget_total", precision = 12, scale = 2)
    private BigDecimal budgetTotal;

    @Column(name = "budget_used", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal budgetUsed = BigDecimal.ZERO;

    @Column(name = "subsidy_bearer", nullable = false, length = 50)
    @Builder.Default
    private String subsidyBearer = "PLATFORM";

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "budget_alert_threshold", precision = 5, scale = 2)
    private BigDecimal budgetAlertThreshold;

    @Column(name = "budget_alert_triggered", nullable = false)
    @Builder.Default
    private Boolean budgetAlertTriggered = false;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromotionRule> rules = new ArrayList<>();

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
