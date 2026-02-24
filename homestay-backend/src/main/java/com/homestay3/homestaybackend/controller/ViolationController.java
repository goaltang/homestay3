package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.ViolationReportDTO;
import com.homestay3.homestaybackend.entity.ViolationAction;
import com.homestay3.homestaybackend.entity.ViolationReport;
import com.homestay3.homestaybackend.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/violations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ViolationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ViolationController.class);
    
    private final ViolationService violationService;
    
    /**
     * 获取违规举报列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getViolationReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String violationType,
            @RequestParam(required = false) String keyword) {
        
        logger.info("获取违规举报列表 - 页码: {}, 大小: {}, 状态: {}, 类型: {}, 关键词: {}", 
                   page, size, status, violationType, keyword);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ViolationReportDTO> reports = violationService.getViolationReports(pageable, status, violationType, keyword);
        
        Map<String, Object> response = Map.of(
            "content", reports.getContent(),
            "totalElements", reports.getTotalElements(),
            "totalPages", reports.getTotalPages(),
            "page", reports.getNumber(),
            "size", reports.getSize()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取待处理的违规举报
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("获取待处理违规举报 - 页码: {}, 大小: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ViolationReportDTO> reports = violationService.getPendingReports(pageable);
        
        Map<String, Object> response = Map.of(
            "content", reports.getContent(),
            "totalElements", reports.getTotalElements(),
            "totalPages", reports.getTotalPages(),
            "page", reports.getNumber(),
            "size", reports.getSize()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取违规举报详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ViolationReportDTO> getReportDetail(@PathVariable Long id) {
        logger.info("获取违规举报详情，ID: {}", id);
        
        ViolationReportDTO report = violationService.getReportDetail(id);
        return ResponseEntity.ok(report);
    }
    
    /**
     * 处理违规举报
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<Map<String, String>> processReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        logger.info("处理违规举报，ID: {}", id);
        
        String actionTypeStr = (String) request.get("actionType");
        String reason = (String) request.get("reason");
        String notes = (String) request.get("notes");
        Integer suspendDays = (Integer) request.get("suspendDays");
        
        ViolationAction.ActionType actionType = ViolationAction.ActionType.valueOf(actionTypeStr);
        String processorUsername = authentication.getName();
        
        violationService.processReport(id, processorUsername, actionType, reason, notes, suspendDays);
        
        return ResponseEntity.ok(Map.of("message", "违规举报处理成功"));
    }
    
    /**
     * 忽略违规举报
     */
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, String>> dismissReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        logger.info("忽略违规举报，ID: {}", id);
        
        String reason = request.get("reason");
        String processorUsername = authentication.getName();
        
        violationService.dismissReport(id, processorUsername, reason);
        
        return ResponseEntity.ok(Map.of("message", "违规举报已忽略"));
    }
    
    /**
     * 获取房源的举报历史
     */
    @GetMapping("/homestay/{homestayId}")
    public ResponseEntity<List<ViolationReportDTO>> getHomestayReports(@PathVariable Long homestayId) {
        logger.info("获取房源举报历史，房源ID: {}", homestayId);
        
        List<ViolationReportDTO> reports = violationService.getHomestayReports(homestayId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * 获取违规统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getViolationStatistics() {
        logger.info("获取违规统计数据");
        
        Map<String, Object> statistics = violationService.getViolationStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 扫描检测可能的违规房源
     */
    @PostMapping("/scan")
    public ResponseEntity<List<Map<String, Object>>> scanForViolations() {
        logger.info("开始违规房源扫描");
        
        List<Map<String, Object>> violations = violationService.scanForViolations();
        return ResponseEntity.ok(violations);
    }
    
    /**
     * 批量处理违规举报
     */
    @PostMapping("/batch-process")
    public ResponseEntity<Map<String, Object>> batchProcessReports(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        logger.info("批量处理违规举报");
        
        @SuppressWarnings("unchecked")
        List<Long> reportIds = (List<Long>) request.get("reportIds");
        String actionTypeStr = (String) request.get("actionType");
        String reason = (String) request.get("reason");
        
        ViolationAction.ActionType actionType = ViolationAction.ActionType.valueOf(actionTypeStr);
        String processorUsername = authentication.getName();
        
        Map<String, Object> result = violationService.batchProcessReports(reportIds, actionType, reason, processorUsername);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取多次被举报的房源
     */
    @GetMapping("/multiple-reports")
    public ResponseEntity<List<Map<String, Object>>> getHomestaysWithMultipleReports(
            @RequestParam(defaultValue = "2") int minReportCount) {
        
        logger.info("获取多次被举报的房源，最少举报次数: {}", minReportCount);
        
        List<Map<String, Object>> homestays = violationService.getHomestaysWithMultipleReports(minReportCount);
        return ResponseEntity.ok(homestays);
    }
} 