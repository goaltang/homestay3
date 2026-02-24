package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.ViolationReportDTO;
import com.homestay3.homestaybackend.entity.ViolationAction;
import com.homestay3.homestaybackend.entity.ViolationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 违规管理服务接口
 */
public interface ViolationService {
    
    /**
     * 提交违规举报
     */
    ViolationReportDTO submitReport(Long homestayId, String reporterUsername, 
                                   ViolationReport.ViolationType violationType, 
                                   String reason, String details, List<String> evidenceImages);
    
    /**
     * 获取违规举报列表（管理员）
     */
    Page<ViolationReportDTO> getViolationReports(Pageable pageable, String status, 
                                                String violationType, String keyword);
    
    /**
     * 获取待处理的违规举报
     */
    Page<ViolationReportDTO> getPendingReports(Pageable pageable);
    
    /**
     * 获取违规举报详情
     */
    ViolationReportDTO getReportDetail(Long reportId);
    
    /**
     * 处理违规举报
     */
    void processReport(Long reportId, String processorUsername, 
                      ViolationAction.ActionType actionType, String reason, 
                      String notes, Integer suspendDays);
    
    /**
     * 忽略违规举报
     */
    void dismissReport(Long reportId, String processorUsername, String reason);
    
    /**
     * 获取房源的举报历史
     */
    List<ViolationReportDTO> getHomestayReports(Long homestayId);
    
    /**
     * 统计违规数据
     */
    Map<String, Object> getViolationStatistics();
    
    /**
     * 扫描检测可能的违规房源
     */
    List<Map<String, Object>> scanForViolations();
    
    /**
     * 批量处理违规举报
     */
    Map<String, Object> batchProcessReports(List<Long> reportIds, 
                                          ViolationAction.ActionType actionType, 
                                          String reason, String processorUsername);
    
    /**
     * 获取多次被举报的房源
     */
    List<Map<String, Object>> getHomestaysWithMultipleReports(int minReportCount);
} 