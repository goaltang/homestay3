package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.OrderService;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 订单自动状态流转服务
 * 处理基于时间的订单状态自动更新，解决状态流转滞后问题
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAutoStatusService {

    private static final String AUTO_CHECKIN_TIME_KEY = "AUTO_CHECKIN_TIME";
    private static final String AUTO_CHECKOUT_TIME_KEY = "AUTO_CHECKOUT_TIME";
    private static final LocalTime DEFAULT_AUTO_CHECKIN_TIME = LocalTime.of(18, 0); // 默认18:00
    private static final LocalTime DEFAULT_AUTO_CHECKOUT_TIME = LocalTime.of(12, 0); // 默认12:00
    private static final LocalTime DEFAULT_CANCEL_MISSED_CHECKIN_TIME = LocalTime.of(12, 0); // 默认12:00

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SystemConfigService systemConfigService;
    
    /**
     * 主要定时任务：处理订单自动状态流转
     * 每小时执行一次，检查所有需要状态更新的订单
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 每小时执行一次
    @Transactional
    public void handleAutoStatusTransition() {
        log.info("开始执行订单自动状态流转检查...");
        
        try {
            // 1. 处理自动入住
            handleAutoCheckIn();
            
            // 2. 处理自动完成
            handleAutoComplete();
            
            // 3. 处理错过入住的订单
            handleMissedCheckIn();
            
            log.info("订单自动状态流转检查完成");
        } catch (Exception e) {
            log.error("订单自动状态流转处理时发生异常", e);
        }
    }
    
    /**
     * 处理自动入住
     * 对于已支付且到达入住时间的订单，自动转为已入住状态
     */
    private void handleAutoCheckIn() {
        LocalDate today = LocalDate.now();
        LocalTime autoCheckinTime = getAutoCheckinTime();
        LocalDateTime checkThreshold = today.atTime(autoCheckinTime);
        
        // 只在指定时间点后执行
        if (LocalDateTime.now().isBefore(checkThreshold)) {
            return;
        }
        
        log.debug("开始处理自动入住，检查入住日期为 {} 的订单", today);
        
        // 查找今天入住的已支付订单和待入住订单
        List<Order> paidOrders = orderRepository.findByStatusAndCheckInDate(
                OrderStatus.PAID.name(), today);
        List<Order> readyOrders = orderRepository.findByStatusAndCheckInDate(
                OrderStatus.READY_FOR_CHECKIN.name(), today);
        
        // 合并两个列表
        paidOrders.addAll(readyOrders);
        
        if (!paidOrders.isEmpty()) {
            log.info("找到 {} 个需要自动入住的订单", paidOrders.size());
            
            for (Order order : paidOrders) {
                try {
                    // 更新为已入住状态
                    orderService.updateOrderStatus(order.getId(), OrderStatus.CHECKED_IN.name());
                    log.info("订单 {} 已自动办理入住", order.getOrderNumber());
                    
                    // TODO: 发送入住确认通知
                    // notificationService.sendAutoCheckInNotification(order);
                    
                } catch (Exception e) {
                    log.error("自动办理入住失败，订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * 处理自动完成
     * 对于已入住且过了退房时间的订单，自动转为已退房状态
     */
    private void handleAutoComplete() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalTime autoCheckoutTime = getAutoCheckoutTime();
        LocalDateTime checkThreshold = LocalDate.now().atTime(autoCheckoutTime);
        
        // 只在指定时间点后执行
        if (LocalDateTime.now().isBefore(checkThreshold)) {
            return;
        }
        
        log.debug("开始处理自动完成，检查退房日期为 {} 的订单", yesterday);
        
        // 查找昨天退房的已入住订单
        List<Order> checkedInOrders = orderRepository.findByStatusAndCheckOutDate(
                OrderStatus.CHECKED_IN.name(), yesterday);
        
        if (!checkedInOrders.isEmpty()) {
            log.info("找到 {} 个需要自动退房的订单", checkedInOrders.size());

            for (Order order : checkedInOrders) {
                try {
                    // 更新为已退房状态
                    orderService.updateOrderStatus(order.getId(), OrderStatus.CHECKED_OUT.name());
                    log.info("订单 {} 已自动办理退房", order.getOrderNumber());

                    // TODO: 发送自动退房通知
                    // notificationService.sendAutoCheckOutNotification(order);

                } catch (Exception e) {
                    log.error("自动退房失败，订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * 处理错过入住的订单
     * 对于已支付但错过入住时间的订单，进行特殊处理
     */
    private void handleMissedCheckIn() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalTime missedCheckinTime = getCancelMissedCheckinTime();
        LocalDateTime checkThreshold = LocalDate.now().atTime(missedCheckinTime);
        
        // 只在指定时间点后执行
        if (LocalDateTime.now().isBefore(checkThreshold)) {
            return;
        }
        
        log.debug("开始处理错过入住的订单，检查入住日期为 {} 的订单", yesterday);
        
        // 查找昨天入住但仍为已支付状态的订单
        List<Order> missedOrders = orderRepository.findByStatusAndCheckInDate(
                OrderStatus.PAID.name(), yesterday);
        List<Order> missedReadyOrders = orderRepository.findByStatusAndCheckInDate(
                OrderStatus.READY_FOR_CHECKIN.name(), yesterday);
        
        missedOrders.addAll(missedReadyOrders);
        
        if (!missedOrders.isEmpty()) {
            log.info("找到 {} 个错过入住的订单", missedOrders.size());
            
            for (Order order : missedOrders) {
                try {
                    // 有两种处理方式：
                    // 1. 直接标记为已入住（假设客户已经入住但房东忘记操作）
                    // 2. 发起退款流程
                    
                    // 这里采用方式1：自动标记为已入住
                    orderService.updateOrderStatus(order.getId(), OrderStatus.CHECKED_IN.name());
                    log.info("错过入住订单 {} 已自动标记为已入住", order.getOrderNumber());
                    
                    // TODO: 发送特殊通知给房东和客户
                    // notificationService.sendMissedCheckInNotification(order);
                    
                } catch (Exception e) {
                    log.error("处理错过入住订单失败，订单ID: {}, 错误: {}", order.getId(), e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * 手动触发自动状态流转（用于测试或紧急情况）
     */
    public void manualTriggerAutoStatusTransition() {
        log.info("手动触发订单自动状态流转...");
        handleAutoStatusTransition();
    }
    
    /**
     * 获取需要处理的订单统计信息
     */
    public AutoStatusStats getAutoStatusStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // 统计各种状态的订单数量
        int pendingCheckIn = orderRepository.countByStatusAndCheckInDate(
                OrderStatus.PAID.name(), today) + 
                orderRepository.countByStatusAndCheckInDate(
                OrderStatus.READY_FOR_CHECKIN.name(), today);
                
        int pendingComplete = orderRepository.countByStatusAndCheckOutDate(
                OrderStatus.CHECKED_IN.name(), yesterday);
                
        int missedCheckIn = orderRepository.countByStatusAndCheckInDate(
                OrderStatus.PAID.name(), yesterday) + 
                orderRepository.countByStatusAndCheckInDate(
                OrderStatus.READY_FOR_CHECKIN.name(), yesterday);
        
        return new AutoStatusStats(pendingCheckIn, pendingComplete, missedCheckIn);
    }
    
    /**
     * 自动状态统计信息
     */
    public static class AutoStatusStats {
        public final int pendingAutoCheckIn;
        public final int pendingAutoComplete;
        public final int missedCheckIn;
        
        public AutoStatusStats(int pendingAutoCheckIn, int pendingAutoComplete, int missedCheckIn) {
            this.pendingAutoCheckIn = pendingAutoCheckIn;
            this.pendingAutoComplete = pendingAutoComplete;
            this.missedCheckIn = missedCheckIn;
        }
    }
    
    /**
     * 分析历史订单，找出需要状态修复的订单
     */
    public Map<String, Object> analyzeHistoryOrders() {
        log.info("开始分析历史订单状态（过去100天）...");
        long startTime = System.currentTimeMillis();
        
        LocalDate today = LocalDate.now();
        Map<String, Object> analysis = new HashMap<>();
        
        // 1. 查找应该已经入住但仍为PAID状态的订单（入住日期在今天之前）
        List<Order> shouldBeCheckedIn = new ArrayList<>();
        for (int i = 1; i <= 100; i++) { // 检查过去100天
            LocalDate pastDate = today.minusDays(i);
            List<Order> paidOrders = orderRepository.findByStatusAndCheckInDate(
                    OrderStatus.PAID.name(), pastDate);
            List<Order> readyOrders = orderRepository.findByStatusAndCheckInDate(
                    OrderStatus.READY_FOR_CHECKIN.name(), pastDate);
            shouldBeCheckedIn.addAll(paidOrders);
            shouldBeCheckedIn.addAll(readyOrders);
        }
        
        // 2. 查找应该已经完成但仍为CHECKED_IN状态的订单（退房日期在今天之前）
        List<Order> shouldBeCompleted = new ArrayList<>();
        for (int i = 1; i <= 100; i++) { // 检查过去100天
            LocalDate pastDate = today.minusDays(i);
            List<Order> checkedInOrders = orderRepository.findByStatusAndCheckOutDate(
                    OrderStatus.CHECKED_IN.name(), pastDate);
            shouldBeCompleted.addAll(checkedInOrders);
        }
        
        // 3. 统计信息
        analysis.put("shouldBeCheckedInCount", shouldBeCheckedIn.size());
        analysis.put("shouldBeCompletedCount", shouldBeCompleted.size());
        analysis.put("totalIssues", shouldBeCheckedIn.size() + shouldBeCompleted.size());
        
        // 4. 详细信息（只返回前10个作为示例）
        analysis.put("shouldBeCheckedInSample", shouldBeCheckedIn.stream()
                .limit(10)
                .map(this::orderToSummary)
                .toList());
        
        analysis.put("shouldBeCompletedSample", shouldBeCompleted.stream()
                .limit(10)
                .map(this::orderToSummary)
                .toList());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        analysis.put("message", "历史订单分析完成");
        analysis.put("recommendation", shouldBeCheckedIn.size() + shouldBeCompleted.size() > 0 ? 
                "发现需要修复的历史订单，建议执行修复操作" : "所有历史订单状态正常");
        analysis.put("analysisTime", duration + "ms");
        analysis.put("dateRange", "过去100天");
        
        log.info("历史订单分析完成: 需要入住状态修复 {} 个，需要完成状态修复 {} 个，耗时 {}ms", 
                shouldBeCheckedIn.size(), shouldBeCompleted.size(), duration);
        
        return analysis;
    }
    
    /**
     * 修复历史订单状态
     */
    public Map<String, Object> fixHistoryOrders() {
        log.info("开始修复历史订单状态...");
        
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new HashMap<>();
        
        int fixedCheckedIn = 0;
        int fixedCompleted = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 1. 修复应该已经入住的订单
            for (int i = 1; i <= 100; i++) { // 处理过去100天
                LocalDate pastDate = today.minusDays(i);
                
                List<Order> paidOrders = orderRepository.findByStatusAndCheckInDate(
                        OrderStatus.PAID.name(), pastDate);
                List<Order> readyOrders = orderRepository.findByStatusAndCheckInDate(
                        OrderStatus.READY_FOR_CHECKIN.name(), pastDate);
                
                // 合并列表
                List<Order> shouldBeCheckedIn = new ArrayList<>();
                shouldBeCheckedIn.addAll(paidOrders);
                shouldBeCheckedIn.addAll(readyOrders);
                
                for (Order order : shouldBeCheckedIn) {
                    try {
                        orderService.updateOrderStatus(order.getId(), OrderStatus.CHECKED_IN.name());
                        fixedCheckedIn++;
                        log.info("历史订单 {} 已修复为已入住状态（原入住日期: {}）", 
                                order.getOrderNumber(), order.getCheckInDate());
                    } catch (Exception e) {
                        failedCount++;
                        String error = String.format("修复订单 %s 失败: %s", order.getOrderNumber(), e.getMessage());
                        errors.add(error);
                        log.error(error, e);
                    }
                }
            }
            
            // 2. 修复应该已经完成的订单
            for (int i = 1; i <= 100; i++) { // 处理过去100天
                LocalDate pastDate = today.minusDays(i);
                
                List<Order> checkedInOrders = orderRepository.findByStatusAndCheckOutDate(
                        OrderStatus.CHECKED_IN.name(), pastDate);
                
                for (Order order : checkedInOrders) {
                    try {
                        orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED.name());
                        fixedCompleted++;
                        log.info("历史订单 {} 已修复为已完成状态（原退房日期: {}）", 
                                order.getOrderNumber(), order.getCheckOutDate());
                    } catch (Exception e) {
                        failedCount++;
                        String error = String.format("修复订单 %s 失败: %s", order.getOrderNumber(), e.getMessage());
                        errors.add(error);
                        log.error(error, e);
                    }
                }
            }
            
            // 3. 返回修复结果
            result.put("fixedCheckedIn", fixedCheckedIn);
            result.put("fixedCompleted", fixedCompleted);
            result.put("totalFixed", fixedCheckedIn + fixedCompleted);
            result.put("failedCount", failedCount);
            result.put("errors", errors);
            result.put("message", String.format("历史订单修复完成: 修复入住状态 %d 个，修复完成状态 %d 个，失败 %d 个", 
                    fixedCheckedIn, fixedCompleted, failedCount));
            
            if (failedCount == 0) {
                result.put("success", true);
            } else {
                result.put("success", false);
                result.put("warning", "部分订单修复失败，请检查错误信息");
            }
            
        } catch (Exception e) {
            log.error("修复历史订单时发生异常", e);
            result.put("success", false);
            result.put("error", "修复过程中发生异常: " + e.getMessage());
        }
        
        log.info("历史订单修复完成: 总计修复 {} 个订单", fixedCheckedIn + fixedCompleted);
        return result;
    }
    
    /**
     * 将订单转换为摘要信息
     */
    private Map<String, Object> orderToSummary(Order order) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", order.getId());
        summary.put("orderNumber", order.getOrderNumber());
        summary.put("status", order.getStatus());
        summary.put("checkInDate", order.getCheckInDate());
        summary.put("checkOutDate", order.getCheckOutDate());
        summary.put("guestId", order.getGuest() != null ? order.getGuest().getId() : null);
        summary.put("homestayId", order.getHomestay() != null ? order.getHomestay().getId() : null);
        return summary;
    }

    /**
     * 获取自动入住时间配置
     */
    private LocalTime getAutoCheckinTime() {
        try {
            String timeStr = systemConfigService.getConfigValue(AUTO_CHECKIN_TIME_KEY);
            if (timeStr != null && !timeStr.isEmpty()) {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }
        } catch (Exception e) {
            log.warn("读取自动入住时间配置失败，使用默认值: {}", e.getMessage());
        }
        return DEFAULT_AUTO_CHECKIN_TIME;
    }

    /**
     * 获取自动退房时间配置
     */
    private LocalTime getAutoCheckoutTime() {
        try {
            String timeStr = systemConfigService.getConfigValue(AUTO_CHECKOUT_TIME_KEY);
            if (timeStr != null && !timeStr.isEmpty()) {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }
        } catch (Exception e) {
            log.warn("读取自动退房时间配置失败，使用默认值: {}", e.getMessage());
        }
        return DEFAULT_AUTO_CHECKOUT_TIME;
    }

    /**
     * 获取错过入住处理时间配置
     */
    private LocalTime getCancelMissedCheckinTime() {
        // 使用退房时间作为错过入住处理时间
        return getAutoCheckoutTime();
    }
} 