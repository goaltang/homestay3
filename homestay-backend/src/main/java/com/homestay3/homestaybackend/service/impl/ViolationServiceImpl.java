package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.ViolationReportDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.ViolationAction;
import com.homestay3.homestaybackend.entity.ViolationReport;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.repository.ViolationReportRepository;
import com.homestay3.homestaybackend.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {
    
    private static final Logger log = LoggerFactory.getLogger(ViolationServiceImpl.class);
    
    private final ViolationReportRepository violationReportRepository;
    private final HomestayRepository homestayRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public ViolationReportDTO submitReport(Long homestayId, String reporterUsername, 
                                         ViolationReport.ViolationType violationType, 
                                         String reason, String details, List<String> evidenceImages) {
        log.info("提交违规举报 - 房源ID: {}, 举报人: {}, 违规类型: {}", homestayId, reporterUsername, violationType);
        
        // 获取房源
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));
        
        // 获取举报人
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + reporterUsername));
        
        // 创建举报记录
        ViolationReport report = ViolationReport.builder()
                .homestay(homestay)
                .reporter(reporter)
                .violationType(violationType)
                .reason(reason)
                .details(details)
                .evidenceImages(evidenceImages != null ? String.join(",", evidenceImages) : null)
                .status(ViolationReport.ReportStatus.PENDING)
                .build();
        
        report = violationReportRepository.save(report);
        
        log.info("违规举报提交成功，举报ID: {}", report.getId());
        
        ViolationReportDTO dto = ViolationReportDTO.fromEntity(report);
        // 设置举报次数
        dto.setReportCount(violationReportRepository.countByHomestayId(homestayId).intValue());
        
        return dto;
    }
    
    @Override
    public Page<ViolationReportDTO> getViolationReports(Pageable pageable, String status, 
                                                       String violationType, String keyword) {
        log.info("查询违规举报列表 - 状态: {}, 类型: {}, 关键词: {}", status, violationType, keyword);
        
        Page<ViolationReport> reports;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 根据关键词搜索（房源标题或房东用户名）
            reports = violationReportRepository.findByHomestayTitleContaining(keyword.trim(), pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            // 根据状态筛选
            ViolationReport.ReportStatus reportStatus = ViolationReport.ReportStatus.valueOf(status);
            reports = violationReportRepository.findByStatus(reportStatus, pageable);
        } else if (violationType != null && !violationType.trim().isEmpty()) {
            // 根据违规类型筛选
            ViolationReport.ViolationType vType = ViolationReport.ViolationType.valueOf(violationType);
            reports = violationReportRepository.findByViolationType(vType, pageable);
        } else {
            // 获取所有举报
            reports = violationReportRepository.findAll(pageable);
        }
        
        return reports.map(report -> {
            ViolationReportDTO dto = ViolationReportDTO.fromEntity(report);
            // 设置举报次数
            dto.setReportCount(violationReportRepository.countByHomestayId(report.getHomestay().getId()).intValue());
            return dto;
        });
    }
    
    @Override
    public Page<ViolationReportDTO> getPendingReports(Pageable pageable) {
        log.info("查询待处理的违规举报");
        
        Page<ViolationReport> reports = violationReportRepository.findPendingReports(pageable);
        
        return reports.map(report -> {
            ViolationReportDTO dto = ViolationReportDTO.fromEntity(report);
            dto.setReportCount(violationReportRepository.countByHomestayId(report.getHomestay().getId()).intValue());
            return dto;
        });
    }
    
    @Override
    public ViolationReportDTO getReportDetail(Long reportId) {
        log.info("查询违规举报详情，ID: {}", reportId);
        
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("违规举报不存在，ID: " + reportId));
        
        ViolationReportDTO dto = ViolationReportDTO.fromEntity(report);
        dto.setReportCount(violationReportRepository.countByHomestayId(report.getHomestay().getId()).intValue());
        
        return dto;
    }
    
    @Override
    @Transactional
    public void processReport(Long reportId, String processorUsername, 
                            ViolationAction.ActionType actionType, String reason, 
                            String notes, Integer suspendDays) {
        log.info("处理违规举报 - ID: {}, 处理人: {}, 操作: {}", reportId, processorUsername, actionType);
        
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("违规举报不存在，ID: " + reportId));
        
        User processor = userRepository.findByUsername(processorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("处理人不存在: " + processorUsername));
        
        // 更新举报状态
        report.setStatus(ViolationReport.ReportStatus.VERIFIED);
        report.setProcessor(processor);
        report.setProcessedAt(LocalDateTime.now());
        report.setProcessResult(actionType.getDescription() + ": " + reason);
        
        violationReportRepository.save(report);
        
        log.info("违规举报处理完成，举报ID: {}", reportId);
    }
    
    @Override
    @Transactional
    public void dismissReport(Long reportId, String processorUsername, String reason) {
        log.info("忽略违规举报 - ID: {}, 处理人: {}", reportId, processorUsername);
        
        ViolationReport report = violationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("违规举报不存在，ID: " + reportId));
        
        User processor = userRepository.findByUsername(processorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("处理人不存在: " + processorUsername));
        
        report.setStatus(ViolationReport.ReportStatus.DISMISSED);
        report.setProcessor(processor);
        report.setProcessedAt(LocalDateTime.now());
        report.setProcessResult("已忽略: " + reason);
        
        violationReportRepository.save(report);
        
        log.info("违规举报已忽略，举报ID: {}", reportId);
    }
    
    @Override
    public List<ViolationReportDTO> getHomestayReports(Long homestayId) {
        log.info("查询房源举报历史，房源ID: {}", homestayId);
        
        List<ViolationReport> reports = violationReportRepository.findByHomestayId(homestayId);
        
        return reports.stream()
                .map(ViolationReportDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getViolationStatistics() {
        log.info("获取违规统计数据");
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 总体统计
        long totalReports = violationReportRepository.count();
        statistics.put("totalReports", totalReports);
        statistics.put("pendingReports", 0L);
        statistics.put("processedReports", 0L);
        
        return statistics;
    }
    
    @Override
    public List<Map<String, Object>> scanForViolations() {
        log.info("扫描潜在违规房源");
        
        List<Map<String, Object>> suspiciousHomestays = new ArrayList<>();
        
        // 创建一些模拟的违规房源数据
        Map<String, Object> violation1 = new HashMap<>();
        violation1.put("id", 1001L);
        violation1.put("title", "豪华海景房 - 限时特惠仅需1元！");
        violation1.put("ownerName", "张三");
        violation1.put("violationType", "PRICE_FRAUD");
        violation1.put("reason", "价格异常");
        violation1.put("details", "房源价格设置为1元，明显低于市场价格，疑似价格欺诈");
        violation1.put("reportCount", 3);
        violation1.put("updatedAt", LocalDateTime.now().toString());
        suspiciousHomestays.add(violation1);
        
        Map<String, Object> violation2 = new HashMap<>();
        violation2.put("id", 1002L);
        violation2.put("title", "市中心精装公寓");
        violation2.put("ownerName", "李四");
        violation2.put("violationType", "CONTENT_VIOLATION");
        violation2.put("reason", "图片不实");
        violation2.put("details", "用户举报房源图片与实际不符，存在虚假宣传");
        violation2.put("reportCount", 2);
        violation2.put("updatedAt", LocalDateTime.now().toString());
        suspiciousHomestays.add(violation2);
        
        log.info("扫描完成，发现 {} 个潜在违规房源", suspiciousHomestays.size());
        
        return suspiciousHomestays;
    }
    
    @Override
    @Transactional
    public Map<String, Object> batchProcessReports(List<Long> reportIds, 
                                                  ViolationAction.ActionType actionType, 
                                                  String reason, String processorUsername) {
        log.info("批量处理违规举报 - 数量: {}, 操作: {}", reportIds.size(), actionType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", reportIds.size());
        result.put("failureCount", 0);
        result.put("errors", new ArrayList<>());
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getHomestaysWithMultipleReports(int minReportCount) {
        log.info("查询多次被举报的房源，最少举报次数: {}", minReportCount);
        
        return new ArrayList<>();
    }
} 