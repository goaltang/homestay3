package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.ViolationReportDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.ViolationAction;
import com.homestay3.homestaybackend.entity.ViolationReport;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.repository.ViolationReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ViolationService 单元测试
 * 测试违规举报核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ViolationServiceImplTest {

    @Mock
    private ViolationReportRepository violationReportRepository;

    @Mock
    private HomestayRepository homestayRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ViolationServiceImpl violationService;

    private User reporter;
    private User processor;
    private Homestay homestay;
    private ViolationReport pendingReport;

    @BeforeEach
    void setUp() {
        // 设置举报人
        reporter = new User();
        reporter.setId(1L);
        reporter.setUsername("reporter");
        reporter.setRealName("举报者");

        // 设置处理人
        processor = new User();
        processor.setId(2L);
        processor.setUsername("admin");
        processor.setRealName("管理员");

        // 设置房源
        homestay = new Homestay();
        homestay.setId(1L);
        homestay.setTitle("测试民宿");
        homestay.setStatus(HomestayStatus.ACTIVE);
        homestay.setOwner(reporter);

        // 设置待处理的举报
        pendingReport = ViolationReport.builder()
                .id(1L)
                .homestay(homestay)
                .reporter(reporter)
                .violationType(ViolationReport.ViolationType.PRICE_FRAUD)
                .reason("价格明显低于市场价")
                .details("该房源标价1元，明显是虚假价格")
                .status(ViolationReport.ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 提交举报功能测试
    // ============================================================

    @Test
    void submitReport_Success() {
        // 准备
        List<String> evidenceImages = Arrays.asList("http://example.com/img1.jpg", "http://example.com/img2.jpg");
        when(homestayRepository.findById(1L)).thenReturn(Optional.of(homestay));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(violationReportRepository.save(any(ViolationReport.class))).thenAnswer(inv -> {
            ViolationReport report = inv.getArgument(0);
            report.setId(1L);
            return report;
        });
        when(violationReportRepository.countByHomestayId(1L)).thenReturn(1L);

        // 执行
        ViolationReportDTO result = violationService.submitReport(
                1L, "reporter", ViolationReport.ViolationType.PRICE_FRAUD,
                "价格明显低于市场价", "该房源标价1元", evidenceImages);

        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PRICE_FRAUD", result.getViolationType());
        assertEquals("价格明显低于市场价", result.getReason());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1, result.getReportCount());
    }

    @Test
    void submitReport_HomestayNotFound() {
        // 准备
        when(homestayRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            violationService.submitReport(999L, "reporter", ViolationReport.ViolationType.PRICE_FRAUD,
                    "原因", null, null);
        });
        assertTrue(exception.getMessage().contains("房源不存在"));
    }

    @Test
    void submitReport_ReporterNotFound() {
        // 准备
        when(homestayRepository.findById(1L)).thenReturn(Optional.of(homestay));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            violationService.submitReport(1L, "nonexistent", ViolationReport.ViolationType.PRICE_FRAUD,
                    "原因", null, null);
        });
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void submitReport_WithNullEvidenceImages() {
        // 准备
        when(homestayRepository.findById(1L)).thenReturn(Optional.of(homestay));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(violationReportRepository.save(any(ViolationReport.class))).thenAnswer(inv -> {
            ViolationReport report = inv.getArgument(0);
            report.setId(1L);
            return report;
        });
        when(violationReportRepository.countByHomestayId(1L)).thenReturn(1L);

        // 执行
        ViolationReportDTO result = violationService.submitReport(
                1L, "reporter", ViolationReport.ViolationType.CONTENT_VIOLATION,
                "内容违规", "详细描述", null);

        // 验证
        assertNotNull(result);
        assertNull(result.getEvidenceImages());
    }

    // ============================================================
    // 获取房源违规记录功能测试
    // ============================================================

    @Test
    void getHomestayReports_Success() {
        // 准备
        ViolationReport report1 = ViolationReport.builder()
                .id(1L)
                .homestay(homestay)
                .reporter(reporter)
                .violationType(ViolationReport.ViolationType.PRICE_FRAUD)
                .reason("原因1")
                .status(ViolationReport.ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        ViolationReport report2 = ViolationReport.builder()
                .id(2L)
                .homestay(homestay)
                .reporter(reporter)
                .violationType(ViolationReport.ViolationType.CONTENT_VIOLATION)
                .reason("原因2")
                .status(ViolationReport.ReportStatus.VERIFIED)
                .createdAt(LocalDateTime.now())
                .build();

        when(violationReportRepository.findByHomestayId(1L)).thenReturn(Arrays.asList(report1, report2));

        // 执行
        List<ViolationReportDTO> results = violationService.getHomestayReports(1L);

        // 验证
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("PRICE_FRAUD", results.get(0).getViolationType());
        assertEquals("CONTENT_VIOLATION", results.get(1).getViolationType());
    }

    @Test
    void getHomestayReports_NoReports() {
        // 准备
        when(violationReportRepository.findByHomestayId(1L)).thenReturn(List.of());

        // 执行
        List<ViolationReportDTO> results = violationService.getHomestayReports(1L);

        // 验证
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ============================================================
    // 处理举报功能测试
    // ============================================================

    @Test
    void processReport_Success() {
        // 准备
        when(violationReportRepository.findById(1L)).thenReturn(Optional.of(pendingReport));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(processor));
        when(violationReportRepository.save(any(ViolationReport.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        violationService.processReport(1L, "admin", ViolationAction.ActionType.WARNING,
                "确认违规", "已确认存在问题", 7);

        // 验证
        ArgumentCaptor<ViolationReport> reportCaptor = ArgumentCaptor.forClass(ViolationReport.class);
        verify(violationReportRepository).save(reportCaptor.capture());

        ViolationReport savedReport = reportCaptor.getValue();
        assertEquals(ViolationReport.ReportStatus.VERIFIED, savedReport.getStatus());
        assertEquals(processor, savedReport.getProcessor());
        assertNotNull(savedReport.getProcessedAt());
        assertTrue(savedReport.getProcessResult().contains("确认违规"));
    }

    @Test
    void processReport_ReportNotFound() {
        // 准备
        when(violationReportRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            violationService.processReport(999L, "admin", ViolationAction.ActionType.WARNING,
                    "原因", null, null);
        });
        assertTrue(exception.getMessage().contains("违规举报不存在"));
    }

    @Test
    void processReport_ProcessorNotFound() {
        // 准备
        when(violationReportRepository.findById(1L)).thenReturn(Optional.of(pendingReport));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            violationService.processReport(1L, "nonexistent", ViolationAction.ActionType.WARNING,
                    "原因", null, null);
        });
        assertTrue(exception.getMessage().contains("处理人不存在"));
    }

    // ============================================================
    // 忽略举报功能测试
    // ============================================================

    @Test
    void dismissReport_Success() {
        // 准备
        when(violationReportRepository.findById(1L)).thenReturn(Optional.of(pendingReport));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(processor));
        when(violationReportRepository.save(any(ViolationReport.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        violationService.dismissReport(1L, "admin", "举报证据不足");

        // 验证
        ArgumentCaptor<ViolationReport> reportCaptor = ArgumentCaptor.forClass(ViolationReport.class);
        verify(violationReportRepository).save(reportCaptor.capture());

        ViolationReport savedReport = reportCaptor.getValue();
        assertEquals(ViolationReport.ReportStatus.DISMISSED, savedReport.getStatus());
        assertEquals(processor, savedReport.getProcessor());
        assertNotNull(savedReport.getProcessedAt());
        assertTrue(savedReport.getProcessResult().contains("已忽略"));
        assertTrue(savedReport.getProcessResult().contains("举报证据不足"));
    }

    @Test
    void dismissReport_ReportNotFound() {
        // 准备
        when(violationReportRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            violationService.dismissReport(999L, "admin", "原因");
        });
        assertTrue(exception.getMessage().contains("违规举报不存在"));
    }

    // ============================================================
    // 获取举报详情功能测试
    // ============================================================

    @Test
    void getReportDetail_Success() {
        // 准备
        when(violationReportRepository.findById(1L)).thenReturn(Optional.of(pendingReport));
        when(violationReportRepository.countByHomestayId(1L)).thenReturn(3L);

        // 执行
        ViolationReportDTO result = violationService.getReportDetail(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PRICE_FRAUD", result.getViolationType());
        assertEquals(3, result.getReportCount());
    }

    @Test
    void getReportDetail_NotFound() {
        // 准备
        when(violationReportRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            violationService.getReportDetail(999L);
        });
        assertTrue(exception.getMessage().contains("违规举报不存在"));
    }

    // ============================================================
    // 获取待处理举报列表功能测试
    // ============================================================

    @Test
    void getPendingReports_Success() {
        // 准备
        Pageable pageable = PageRequest.of(0, 10);
        Page<ViolationReport> reportPage = new PageImpl<>(List.of(pendingReport), pageable, 1);

        when(violationReportRepository.findPendingReports(pageable)).thenReturn(reportPage);
        when(violationReportRepository.countByHomestayId(1L)).thenReturn(1L);

        // 执行
        Page<ViolationReportDTO> result = violationService.getPendingReports(pageable);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
    }

    // ============================================================
    // 获取违规统计数据功能测试
    // ============================================================

    @Test
    void getViolationStatistics_Success() {
        // 准备
        when(violationReportRepository.count()).thenReturn(10L);

        // 执行
        var statistics = violationService.getViolationStatistics();

        // 验证
        assertNotNull(statistics);
        assertEquals(10L, statistics.get("totalReports"));
        assertEquals(0L, statistics.get("pendingReports"));
        assertEquals(0L, statistics.get("processedReports"));
    }

    // ============================================================
    // 扫描违规房源功能测试
    // ============================================================

    @Test
    void scanForViolations_Success() {
        // 执行
        List<Map<String, Object>> results = violationService.scanForViolations();

        // 验证
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("PRICE_FRAUD", results.get(0).get("violationType"));
        assertEquals("CONTENT_VIOLATION", results.get(1).get("violationType"));
    }

    // ============================================================
    // 批量处理举报功能测试
    // ============================================================

    @Test
    void batchProcessReports_Success() {
        // 准备
        List<Long> reportIds = Arrays.asList(1L, 2L, 3L);

        // 执行
        Map<String, Object> result = violationService.batchProcessReports(
                reportIds, ViolationAction.ActionType.WARNING, "批量警告", "admin");

        // 验证
        assertNotNull(result);
        assertEquals(3, result.get("successCount"));
        assertEquals(0, result.get("failureCount"));
    }

    @Test
    void batchProcessReports_EmptyList() {
        // 准备
        List<Long> reportIds = List.of();

        // 执行
        Map<String, Object> result = violationService.batchProcessReports(
                reportIds, ViolationAction.ActionType.WARNING, "批量警告", "admin");

        // 验证
        assertNotNull(result);
        assertEquals(0, result.get("successCount"));
    }
}
