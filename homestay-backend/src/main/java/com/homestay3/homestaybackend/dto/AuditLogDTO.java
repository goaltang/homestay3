package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.model.HomestayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审核记录DTO
 * 用于返回房源审核历史记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    private Long id;
    private Long homestayId;
    private String homestayTitle;
    private Long reviewerId;
    private String reviewerName;
    private HomestayStatus oldStatus;
    private HomestayStatus newStatus;
    private HomestayAuditLog.AuditActionType actionType;
    private String reviewReason;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private String ipAddress;
} 