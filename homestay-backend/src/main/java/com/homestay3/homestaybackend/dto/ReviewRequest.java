package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审核请求DTO
 * 用于提交房源审核操作的请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    /**
     * 审核操作类型
     */
    @NotNull(message = "审核操作类型不能为空")
    private HomestayAuditLog.AuditActionType actionType;

    /**
     * 目标状态（批准时为ACTIVE，拒绝时为REJECTED）
     * 只在actionType不是APPROVE或REJECT时需要指定
     */
    private String targetStatus;

    /**
     * 审核原因/拒绝原因
     */
    @Size(max = 1000, message = "审核原因不能超过1000个字符")
    private String reviewReason;

    /**
     * 审核员备注
     */
    @Size(max = 1000, message = "审核员备注不能超过1000个字符")
    private String reviewNotes;


} 