package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AuditLogDTO;
import com.homestay3.homestaybackend.dto.ReviewRequest;
import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.service.HomestayAuditService;
import com.homestay3.homestaybackend.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 房源审核控制器
 * 负责房源审核相关的所有操作，包括提交审核、撤回审核、审核操作、批量审核等
 */
@RestController
@RequestMapping({"/api/homestays", "/api/v1/homestays"})
@RequiredArgsConstructor
public class HomestayAuditController {

    private static final Logger logger = LoggerFactory.getLogger(HomestayAuditController.class);

    private final HomestayAuditService homestayAuditService;

    /**
     * 新的房源审核接口
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> reviewHomestay(
            @PathVariable Long id,
            @RequestBody @Valid ReviewRequest reviewRequest,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            String reviewerUsername = authentication.getName();
            String ipAddress = IpUtil.getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            AuditLogDTO auditLog = homestayAuditService.reviewHomestay(
                id, reviewRequest, reviewerUsername, ipAddress, userAgent);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审核操作完成",
                "auditLog", auditLog
            ));
        } catch (Exception e) {
            logger.error("审核房源失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取房源审核历史
     */
    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HOST')")
    public ResponseEntity<Page<AuditLogDTO>> getHomestayAuditLogs(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogDTO> auditLogs = homestayAuditService.getHomestayAuditHistory(id, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * 提交房源审核
     */
    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
    public ResponseEntity<?> submitHomestayForReview(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String ownerUsername = authentication.getName();
            AuditLogDTO auditLog = homestayAuditService.submitHomestayForReview(id, ownerUsername);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "房源已提交审核",
                "auditLog", auditLog
            ));
        } catch (Exception e) {
            logger.error("提交房源审核失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 撤回房源审核申请
     */
    @PostMapping("/{id}/withdraw-review")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
    public ResponseEntity<?> withdrawHomestayReview(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String ownerUsername = authentication.getName();
            AuditLogDTO auditLog = homestayAuditService.withdrawHomestayReview(id, ownerUsername);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审核申请已撤回",
                "auditLog", auditLog
            ));
        } catch (Exception e) {
            logger.error("撤回房源审核失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 批量审核房源
     */
    @PostMapping("/batch/review")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> batchReviewHomestays(
            @RequestBody Map<String, Object> request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        try {
            // 安全的类型转换，处理Integer到Long的转换
            @SuppressWarnings("unchecked")
            List<Object> homestayIdsRaw = (List<Object>) request.get("homestayIds");
            List<Long> homestayIds = null;
            
            if (homestayIdsRaw != null) {
                homestayIds = homestayIdsRaw.stream()
                    .map(obj -> {
                        if (obj instanceof Integer) {
                            return ((Integer) obj).longValue();
                        } else if (obj instanceof Long) {
                            return (Long) obj;
                        } else if (obj instanceof String) {
                            return Long.parseLong((String) obj);
                        } else {
                            throw new IllegalArgumentException("Invalid homestay ID type: " + obj.getClass());
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            String actionType = (String) request.get("actionType");
            String reviewReason = (String) request.get("reviewReason");
            String reviewNotes = (String) request.get("reviewNotes");

            if (homestayIds == null || homestayIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "房源ID列表不能为空"
                ));
            }

            if (actionType == null || (!actionType.equals("APPROVE") && !actionType.equals("REJECT"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无效的操作类型"
                ));
            }

            String reviewerUsername = authentication.getName();
            String ipAddress = IpUtil.getClientIpAddress(servletRequest);
            String userAgent = servletRequest.getHeader("User-Agent");

            // 批量审核处理
            int successCount = 0;
            int failureCount = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (Long homestayId : homestayIds) {
                try {
                    ReviewRequest reviewRequest = new ReviewRequest();
                    reviewRequest.setActionType(HomestayAuditLog.AuditActionType.valueOf(actionType));
                    reviewRequest.setReviewReason(reviewReason);
                    reviewRequest.setReviewNotes(reviewNotes);
                    
                    // 根据操作类型设置目标状态
                    if ("APPROVE".equals(actionType)) {
                        reviewRequest.setTargetStatus("ACTIVE");
                    } else if ("REJECT".equals(actionType)) {
                        reviewRequest.setTargetStatus("REJECTED");
                    }

                    AuditLogDTO auditLog = homestayAuditService.reviewHomestay(
                        homestayId, reviewRequest, reviewerUsername, ipAddress, userAgent);

                    results.add(Map.of(
                        "homestayId", homestayId,
                        "success", true,
                        "auditLog", auditLog
                    ));
                    successCount++;

                } catch (Exception e) {
                    logger.error("批量审核房源失败，ID: {}, 错误: {}", homestayId, e.getMessage());
                    results.add(Map.of(
                        "homestayId", homestayId,
                        "success", false,
                        "error", e.getMessage()
                    ));
                    failureCount++;
                }
            }

            logger.info("批量审核完成，成功: {}, 失败: {}", successCount, failureCount);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("批量审核完成，成功 %d 个，失败 %d 个", successCount, failureCount),
                "successCount", successCount,
                "failureCount", failureCount,
                "results", results
            ));

        } catch (Exception e) {
            logger.error("批量审核房源失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "批量审核失败: " + e.getMessage()
            ));
        }
    }
}
