package com.homestay3.homestaybackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_broadcast_jobs", indexes = {
        @Index(name = "idx_notification_broadcast_status_submitted", columnList = "status, submitted_at"),
        @Index(name = "idx_notification_broadcast_initiator_submitted", columnList = "initiated_by, submitted_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationBroadcastJob {

    public enum Status {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED,
        RATE_LIMITED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Column(name = "initiated_by")
    private Long initiatedBy;

    @Column(name = "initiated_by_username", length = 100)
    private String initiatedByUsername;

    @Column(name = "content_summary", nullable = false, length = 255)
    private String contentSummary;

    @Column(name = "content_length", nullable = false)
    private int contentLength;

    @Builder.Default
    @Column(name = "target_count", nullable = false)
    private int targetCount = 0;

    @Builder.Default
    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Builder.Default
    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
