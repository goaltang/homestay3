package com.homestay3.homestaybackend.service;

import java.time.LocalDate;
import java.util.Map;

public interface StatisticsService {
    
    /**
     * 获取管理员统计数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据
     */
    Map<String, Object> getAdminStatistics(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取订单趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单趋势数据
     */
    Map<String, Object> getOrderTrend(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取收入趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收入趋势数据
     */
    Map<String, Object> getIncomeTrend(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取用户增长趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户增长趋势数据
     */
    Map<String, Object> getUserGrowthTrend(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取民宿地区分布
     * @return 民宿地区分布数据
     */
    Map<String, Object> getHomestayDistribution();
    
    /**
     * 导出统计数据
     */
    byte[] exportStatistics(LocalDate startDate, LocalDate endDate);
} 