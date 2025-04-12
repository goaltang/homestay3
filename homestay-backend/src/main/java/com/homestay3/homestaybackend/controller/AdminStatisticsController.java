package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(AdminStatisticsController.class);
    private final StatisticsService statisticsService;

    /**
     * 获取统计数据总览
     */
    @GetMapping
    public ResponseEntity<?> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("管理员获取统计数据, 开始日期: {}, 结束日期: {}", startDate, endDate);
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, Object> statistics = statisticsService.getAdminStatistics(start, end);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取订单趋势
     */
    @GetMapping("/order-trend")
    public ResponseEntity<?> getOrderTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("管理员获取订单趋势, 开始日期: {}, 结束日期: {}", startDate, endDate);
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, Object> trendData = statisticsService.getOrderTrend(start, end);
        return ResponseEntity.ok(trendData);
    }

    /**
     * 获取收入趋势
     */
    @GetMapping("/income-trend")
    public ResponseEntity<?> getIncomeTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("管理员获取收入趋势, 开始日期: {}, 结束日期: {}", startDate, endDate);
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, Object> trendData = statisticsService.getIncomeTrend(start, end);
        return ResponseEntity.ok(trendData);
    }

    /**
     * 获取用户增长趋势
     */
    @GetMapping("/user-trend")
    public ResponseEntity<?> getUserTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("管理员获取用户增长趋势, 开始日期: {}, 结束日期: {}", startDate, endDate);
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, Object> trendData = statisticsService.getUserGrowthTrend(start, end);
        return ResponseEntity.ok(trendData);
    }

    /**
     * 获取民宿地区分布
     */
    @GetMapping("/homestay-distribution")
    public ResponseEntity<?> getHomestayDistribution() {
        logger.info("管理员获取民宿地区分布");
        Map<String, Object> distributionData = statisticsService.getHomestayDistribution();
        return ResponseEntity.ok(distributionData);
    }

    /**
     * 导出统计数据
     */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("管理员导出统计数据, 开始日期: {}, 结束日期: {}", startDate, endDate);
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        byte[] data = statisticsService.exportStatistics(start, end);
        String filename = "statistics_" + start.format(DateTimeFormatter.ISO_DATE) + 
                "_to_" + end.format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }
} 