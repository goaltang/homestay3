package com.homestay3.homestaybackend.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 争议记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRecordDTO {

    private Long id;

    private Long orderId;

    private String orderNumber;

    private String disputeReason;

    private Long raisedBy;

    private String raisedByUsername;

    private LocalDateTime raisedAt;

    private String resolution;

    private String resolutionText;

    private Long resolvedBy;

    private String resolvedByUsername;

    private LocalDateTime resolvedAt;

    private String resolutionNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 订单相关信息
    private String homestayTitle;

    private String guestUsername;

    private String hostUsername;

    private Double refundAmount;

    private String refundReason;
}
