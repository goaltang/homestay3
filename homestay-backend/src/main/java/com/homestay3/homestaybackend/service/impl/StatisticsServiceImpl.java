package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);
    
    private final HomestayRepository homestayRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    
    @Override
    public Map<String, Object> getAdminStatistics(LocalDate startDate, LocalDate endDate) {
        logger.info("获取管理员统计数据，开始日期：{}，结束日期：{}", startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        
        // 基础统计
        long homestayCount = homestayRepository.count();
        long orderCount = orderRepository.count();
        long userCount = userRepository.count();
        
        // 周期内统计
        long newHomestaysInPeriod = homestayRepository.countByCreatedAtBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        long newOrdersInPeriod = orderRepository.countByCreatedAtBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        long newUsersInPeriod = userRepository.countByCreatedAtBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        
        // 今日统计
        LocalDate today = LocalDate.now();
        long todayHomestays = homestayRepository.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long todayOrders = orderRepository.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long todayUsers = userRepository.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        
        // 设置结果
        result.put("total", Map.of(
            "homestays", homestayCount,
            "orders", orderCount,
            "users", userCount
        ));
        
        result.put("period", Map.of(
            "newHomestays", newHomestaysInPeriod,
            "newOrders", newOrdersInPeriod,
            "newUsers", newUsersInPeriod
        ));
        
        result.put("today", Map.of(
            "newHomestays", todayHomestays,
            "newOrders", todayOrders,
            "newUsers", todayUsers
        ));
        
        return result;
    }
    
    @Override
    public Map<String, Object> getOrderTrend(LocalDate startDate, LocalDate endDate) {
        logger.info("获取订单趋势，开始日期：{}，结束日期：{}", startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 准备日期列表
        List<String> dateList = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        
        // 计算每一天的订单数
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(formatter);
            dateList.add(dateStr);
            
            LocalDate nextDate = currentDate.plusDays(1);
            long count = orderRepository.countByCreatedAtBetween(currentDate.atStartOfDay(), nextDate.atStartOfDay());
            orderCounts.add(count);
            
            currentDate = nextDate;
        }
        
        result.put("dates", dateList);
        result.put("counts", orderCounts);
        
        return result;
    }
    
    @Override
    public Map<String, Object> getIncomeTrend(LocalDate startDate, LocalDate endDate) {
        logger.info("获取收入趋势，开始日期：{}，结束日期：{}", startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 准备日期列表
        List<String> dateList = new ArrayList<>();
        List<Double> incomeAmounts = new ArrayList<>();
        
        // 计算每一天的收入
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(formatter);
            dateList.add(dateStr);
            
            LocalDate nextDate = currentDate.plusDays(1);
            double amount = orderRepository.sumTotalPriceByCreatedAtBetweenAndStatus(
                currentDate.atStartOfDay(), 
                nextDate.atStartOfDay(),
                "COMPLETED"
            ).orElse(0.0);
            
            incomeAmounts.add(amount);
            
            currentDate = nextDate;
        }
        
        result.put("dates", dateList);
        result.put("amounts", incomeAmounts);
        
        return result;
    }
    
    @Override
    public Map<String, Object> getUserGrowthTrend(LocalDate startDate, LocalDate endDate) {
        logger.info("获取用户增长趋势，开始日期：{}，结束日期：{}", startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 准备日期列表
        List<String> dateList = new ArrayList<>();
        List<Long> userCounts = new ArrayList<>();
        
        // 计算累计用户数
        long totalUsersBefore = userRepository.countByCreatedAtBefore(startDate.atStartOfDay());
        
        // 计算每一天的新增用户和累计用户
        LocalDate currentDate = startDate;
        long accumulatedUsers = totalUsersBefore;
        
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(formatter);
            dateList.add(dateStr);
            
            LocalDate nextDate = currentDate.plusDays(1);
            long newUsers = userRepository.countByCreatedAtBetween(currentDate.atStartOfDay(), nextDate.atStartOfDay());
            accumulatedUsers += newUsers;
            
            userCounts.add(accumulatedUsers);
            
            currentDate = nextDate;
        }
        
        result.put("dates", dateList);
        result.put("counts", userCounts);
        
        return result;
    }
    
    @Override
    public Map<String, Object> getHomestayDistribution() {
        logger.info("获取民宿地区分布");
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取地区分布
        List<Object[]> distributionData = homestayRepository.countByProvince();
        
        List<String> provinces = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        
        for (Object[] data : distributionData) {
            String province = (String) data[0];
            Long count = (Long) data[1];
            
            if (province != null && !province.isEmpty()) {
                provinces.add(province);
                counts.add(count);
            }
        }
        
        result.put("provinces", provinces);
        result.put("counts", counts);
        
        return result;
    }
    
    @Override
    public byte[] exportStatistics(LocalDate startDate, LocalDate endDate) {
        logger.info("导出统计数据，开始日期：{}，结束日期：{}", startDate, endDate);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            // 收集统计数据
            Map<String, Object> statistics = getAdminStatistics(startDate, endDate);
            Map<String, Object> orderTrend = getOrderTrend(startDate, endDate);
            Map<String, Object> incomeTrend = getIncomeTrend(startDate, endDate);
            Map<String, Object> userTrend = getUserGrowthTrend(startDate, endDate);
            
            // 格式化数据为CSV格式
            StringBuilder csvContent = new StringBuilder();
            
            // 添加标题
            csvContent.append("民宿管理系统统计报表\n");
            csvContent.append("统计周期: ").append(startDate).append(" 至 ").append(endDate).append("\n\n");
            
            // 基本统计数据
            @SuppressWarnings("unchecked")
            Map<String, Object> totalStats = (Map<String, Object>) statistics.get("total");
            csvContent.append("总览数据:\n");
            csvContent.append("总房源数,").append(totalStats.get("homestays")).append("\n");
            csvContent.append("总订单数,").append(totalStats.get("orders")).append("\n");
            csvContent.append("总用户数,").append(totalStats.get("users")).append("\n\n");
            
            // 周期内统计
            @SuppressWarnings("unchecked")
            Map<String, Object> periodStats = (Map<String, Object>) statistics.get("period");
            csvContent.append("周期内新增:\n");
            csvContent.append("新增房源,").append(periodStats.get("newHomestays")).append("\n");
            csvContent.append("新增订单,").append(periodStats.get("newOrders")).append("\n");
            csvContent.append("新增用户,").append(periodStats.get("newUsers")).append("\n\n");
            
            // 订单趋势
            csvContent.append("订单趋势:\n");
            csvContent.append("日期,订单数\n");
            @SuppressWarnings("unchecked")
            List<String> orderDates = (List<String>) orderTrend.get("dates");
            @SuppressWarnings("unchecked")
            List<Long> orderCounts = (List<Long>) orderTrend.get("counts");
            for (int i = 0; i < orderDates.size(); i++) {
                csvContent.append(orderDates.get(i)).append(",").append(orderCounts.get(i)).append("\n");
            }
            csvContent.append("\n");
            
            // 收入趋势
            csvContent.append("收入趋势:\n");
            csvContent.append("日期,收入金额\n");
            @SuppressWarnings("unchecked")
            List<String> incomeDates = (List<String>) incomeTrend.get("dates");
            @SuppressWarnings("unchecked")
            List<Double> incomeAmounts = (List<Double>) incomeTrend.get("amounts");
            for (int i = 0; i < incomeDates.size(); i++) {
                csvContent.append(incomeDates.get(i)).append(",").append(incomeAmounts.get(i)).append("\n");
            }
            csvContent.append("\n");
            
            // 用户增长趋势
            csvContent.append("用户增长趋势:\n");
            csvContent.append("日期,累计用户数\n");
            @SuppressWarnings("unchecked")
            List<String> userDates = (List<String>) userTrend.get("dates");
            @SuppressWarnings("unchecked")
            List<Long> userCounts = (List<Long>) userTrend.get("counts");
            for (int i = 0; i < userDates.size(); i++) {
                csvContent.append(userDates.get(i)).append(",").append(userCounts.get(i)).append("\n");
            }
            
            // 导出为UTF-8编码的字节数组
            byte[] bytes = csvContent.toString().getBytes(StandardCharsets.UTF_8);
            outputStream.write(bytes);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("导出统计数据失败", e);
            throw new RuntimeException("导出统计数据失败", e);
        }
    }
} 