package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.ViolationReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 违规举报DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationReportDTO {
    
    private Long id;
    
    // 房源信息
    private Long homestayId;
    private String homestayTitle;
    private String homestayStatus;
    private String ownerName;
    private String ownerUsername;
    
    // 举报人信息
    private Long reporterId;
    private String reporterName;
    private String reporterUsername;
    
    // 违规信息
    private String violationType;
    private String violationTypeName;
    private String reason;
    private String details;
    private List<String> evidenceImages;
    
    // 状态信息
    private String status;
    private String statusName;
    private String processResult;
    
    // 处理信息
    private Long processorId;
    private String processorName;
    private LocalDateTime processedAt;
    
    // 时间信息
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 统计信息
    private Integer reportCount; // 该房源被举报的总次数
    
    /**
     * 从实体转换为DTO
     */
    public static ViolationReportDTO fromEntity(ViolationReport report) {
        if (report == null) {
            return null;
        }
        
        ViolationReportDTO dto = ViolationReportDTO.builder()
                .id(report.getId())
                .violationType(report.getViolationType().name())
                .violationTypeName(report.getViolationType().getDescription())
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus().name())
                .statusName(report.getStatus().getDescription())
                .processResult(report.getProcessResult())
                .processedAt(report.getProcessedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
        
        // 设置房源信息
        if (report.getHomestay() != null) {
            dto.setHomestayId(report.getHomestay().getId());
            dto.setHomestayTitle(report.getHomestay().getTitle());
            dto.setHomestayStatus(report.getHomestay().getStatus().name());
            
            if (report.getHomestay().getOwner() != null) {
                dto.setOwnerName(report.getHomestay().getOwner().getRealName() != null 
                    ? report.getHomestay().getOwner().getRealName() 
                    : report.getHomestay().getOwner().getUsername());
                dto.setOwnerUsername(report.getHomestay().getOwner().getUsername());
            }
        }
        
        // 设置举报人信息
        if (report.getReporter() != null) {
            dto.setReporterId(report.getReporter().getId());
            dto.setReporterName(report.getReporter().getRealName() != null 
                ? report.getReporter().getRealName() 
                : report.getReporter().getUsername());
            dto.setReporterUsername(report.getReporter().getUsername());
        }
        
        // 设置处理人信息
        if (report.getProcessor() != null) {
            dto.setProcessorId(report.getProcessor().getId());
            dto.setProcessorName(report.getProcessor().getRealName() != null 
                ? report.getProcessor().getRealName() 
                : report.getProcessor().getUsername());
        }
        
        // 处理证据图片
        if (report.getEvidenceImages() != null && !report.getEvidenceImages().isEmpty()) {
            dto.setEvidenceImages(List.of(report.getEvidenceImages().split(",")));
        }
        
        return dto;
    }
} 