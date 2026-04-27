package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rule",
        indexes = {
                @Index(name = "idx_scope", columnList = "scope_type, enabled"),
                @Index(name = "idx_rule_type", columnList = "rule_type, enabled"),
                @Index(name = "idx_date_range", columnList = "start_date, end_date")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "scope_type", nullable = false, length = 50)
    private String scopeType;

    @Column(name = "scope_value_json", columnDefinition = "TEXT")
    private String scopeValueJson;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "adjustment_type", nullable = false, length = 50)
    private String adjustmentType;

    @Column(name = "adjustment_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal adjustmentValue;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean stackable = true;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "min_nights")
    private Integer minNights;

    @Column(name = "max_nights")
    private Integer maxNights;

    @Column(name = "min_advance_days")
    private Integer minAdvanceDays;

    @Column(name = "max_advance_days")
    private Integer maxAdvanceDays;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
