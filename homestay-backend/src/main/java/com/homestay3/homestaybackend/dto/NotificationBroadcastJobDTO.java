package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBroadcastJobDTO {
    private Long jobId;
    private NotificationBroadcastJob.Status status;
    private Long initiatedBy;
    private String initiatedByUsername;
    private String contentSummary;
    private int contentLength;
    private int targetCount;
    private int successCount;
    private int failureCount;
    private String failureReason;
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static NotificationBroadcastJobDTO fromEntity(NotificationBroadcastJob job) {
        return NotificationBroadcastJobDTO.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .initiatedBy(job.getInitiatedBy())
                .initiatedByUsername(job.getInitiatedByUsername())
                .contentSummary(job.getContentSummary())
                .contentLength(job.getContentLength())
                .targetCount(job.getTargetCount())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .failureReason(job.getFailureReason())
                .submittedAt(job.getSubmittedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
