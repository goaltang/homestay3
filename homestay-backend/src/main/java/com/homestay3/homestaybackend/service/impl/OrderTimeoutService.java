package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.IOrderTimeoutService;
import com.homestay3.homestaybackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时处理服务
 * 处理订单在各个状态的超时情况
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutService implements IOrderTimeoutService {
    
    private final OrderRepository orderRepository;
    
    // 注意：直接注入 OrderServiceImpl 可能导致循环依赖，更好的方式是注入 OrderService 接口
    // private final OrderServiceImpl orderService; 
    private final OrderService orderService;
    
    /**
     * 超时配置（单位：小时）
     */
    private static final int PENDING_TIMEOUT_HOURS = 24; // 待确认状态超时时间
    private static final int CONFIRMED_TIMEOUT_HOURS = 2; // 已确认未支付状态超时时间
    private static final int PAYMENT_PENDING_TIMEOUT_HOURS = 2; // 支付中状态超时时间 (新增)
    
    /**
     * 定时任务：处理超时订单
     * 每小时执行一次
     */
    @Override
    @Scheduled(fixedRate = 60 * 60 * 1000) // 每小时执行一次
    @Transactional
    public void handleTimeoutOrders() {
        log.info("开始处理超时订单...");
        
        // 处理待确认超时订单
        handlePendingTimeoutOrders();
        
        // 处理已确认未支付超时订单
        handleConfirmedTimeoutOrders();
        
        // 处理支付中超时订单 (新增)
        handlePaymentPendingTimeoutOrders();
        
        log.info("超时订单处理完成");
    }
    
    /**
     * 处理待确认超时订单
     */
    private void handlePendingTimeoutOrders() {
        // 获取所有待确认且已经超时的订单
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(PENDING_TIMEOUT_HOURS);
        log.debug("查找创建时间早于 {} 的 PENDING 订单", timeoutThreshold);
        List<Order> timeoutOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING.name(), timeoutThreshold);
        
        if (!timeoutOrders.isEmpty()) {
            log.info("找到 {} 个待确认超时订单", timeoutOrders.size());
            
            // 更新订单状态为系统取消
            for (Order order : timeoutOrders) {
                try {
                    // 使用 OrderService 接口的方法
                    orderService.cancelOrderWithReason(order.getId(), OrderStatus.CANCELLED_SYSTEM.name(), "系统自动取消原因: 24小时内未确认");
                    log.info("已自动取消超时待确认订单: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("取消超时待确认订单失败: {}, 错误: {}", order.getId(), e.getMessage(), e); // 添加异常堆栈
                }
            }
        } else {
            log.debug("没有待确认超时订单");
        }
    }
    
    /**
     * 处理已确认未支付超时订单
     */
    private void handleConfirmedTimeoutOrders() {
        // 获取所有已确认且已经超时的订单
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(CONFIRMED_TIMEOUT_HOURS);
         log.debug("查找更新时间早于 {} 的 CONFIRMED 订单", timeoutThreshold);
        List<Order> timeoutOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                OrderStatus.CONFIRMED.name(), timeoutThreshold);
        
        if (!timeoutOrders.isEmpty()) {
            log.info("找到 {} 个已确认未支付超时订单", timeoutOrders.size());
            
            // 更新订单状态为系统取消
            for (Order order : timeoutOrders) {
                try {
                    // 使用 OrderService 接口的方法
                    orderService.cancelOrderWithReason(order.getId(), OrderStatus.CANCELLED_SYSTEM.name(), "系统自动取消原因: 2小时内未支付");
                    log.info("已自动取消超时未支付订单: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("取消超时未支付订单失败: {}, 错误: {}", order.getId(), e.getMessage(), e); // 添加异常堆栈
                }
            }
        } else {
            log.debug("没有已确认未支付超时订单");
        }
    }
    
    /**
     * 处理支付中超时订单 (新增)
     */
    private void handlePaymentPendingTimeoutOrders() {
        // 获取所有支付中且已经超时的订单 (通常支付中状态更新时间较新，用 updatedAt 比较合适)
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(PAYMENT_PENDING_TIMEOUT_HOURS);
        log.debug("查找更新时间早于 {} 的 PAYMENT_PENDING 订单", timeoutThreshold);
        List<Order> timeoutOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                OrderStatus.PAYMENT_PENDING.name(), timeoutThreshold);
        
        if (!timeoutOrders.isEmpty()) {
            log.info("找到 {} 个支付中超时订单", timeoutOrders.size());
            
            // 更新订单状态为系统取消
            for (Order order : timeoutOrders) {
                try {
                     // 使用 OrderService 接口的方法
                    orderService.cancelOrderWithReason(order.getId(), OrderStatus.CANCELLED_SYSTEM.name(), "系统自动取消原因: 支付超时");
                    log.info("已自动取消支付超时订单: {}", order.getOrderNumber());
                } catch (Exception e) {
                    log.error("取消支付超时订单失败: {}, 错误: {}", order.getId(), e.getMessage(), e); // 添加异常堆栈
                }
            }
        } else {
            log.debug("没有支付中超时订单");
        }
    }
} 