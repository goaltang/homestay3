package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预订冲突检测服务
 * 提供基于 Redis 分布式锁的并发控制机制，防止重复预订
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingConflictService {

    private final OrderRepository orderRepository;
    private final HomestayAvailabilityService availabilityService;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;

    private static final String LOCK_KEY_PREFIX = "lock:homestay:%d:date:%s";

    /**
     * 安全地创建订单（在 Redis 分布式锁保护下）
     * 使用 RedissonMultiLock 确保多日期锁定的原子性
     * 支持幂等性：如果传入 idempotencyKey 且已存在，则返回已存在的订单
     * 
     * @param order 订单对象
     * @param idempotencyKey 幂等性 key（可选）
     * @return 保存的订单
     */
    public Order safeCreateOrder(Order order, String idempotencyKey) {
        Long homestayId = order.getHomestay().getId();
        LocalDate checkIn = order.getCheckInDate();
        LocalDate checkOut = order.getCheckOutDate();

        // 幂等性检查：如果提供了 idempotencyKey，检查是否已存在
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existingOrder != null) {
                log.info("检测到重复请求，幂等返回已有订单: {}", existingOrder.getOrderNumber());
                return existingOrder;
            }
        }

        List<RLock> locks = new ArrayList<>();

        try {
            // 1. 获取日期范围内的所有锁对象
            for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
                String lockKey = String.format(LOCK_KEY_PREFIX, homestayId, date.toString());
                locks.add(redissonClient.getLock(lockKey));
            }

            // 2. 使用 MultiLock 一次性锁定所有日期
            RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

            boolean locked = false;
            try {
                // 尝试获取锁，等待10秒，锁定30秒
                locked = multiLock.tryLock(10, 30, TimeUnit.SECONDS);
                if (!locked) {
                    throw new RuntimeException("获取房源日期锁超时，系统繁忙，请稍后重试");
                }

                // 3. 在锁保护下执行数据库操作 (使用 TransactionTemplate 确保事务生效)
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                return transactionTemplate.execute(status -> executeInternal(order, idempotencyKey));
            } finally {
                // 确保锁在获取成功的情况下释放
                if (locked) {
                    multiLock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("订单创建操作被中断");
        }
    }

    /**
     * 安全地创建订单（无幂等性检查的版本）
     * 
     * @param order 订单对象
     * @return 保存的订单
     */
    public Order safeCreateOrder(Order order) {
        return safeCreateOrder(order, null);
    }

    private Order executeInternal(Order order, String idempotencyKey) {
        Long homestayId = order.getHomestay().getId();
        
        // 第一道防线：数据库查询检查
        boolean hasConflict = orderRepository.existsOverlappingBooking(
                homestayId, order.getCheckInDate(), order.getCheckOutDate());

        if (hasConflict) {
            log.warn("订单创建失败：数据库检测到日期冲突 (homestayId={}, checkIn={}, checkOut={})",
                    homestayId, order.getCheckInDate(), order.getCheckOutDate());
            throw new IllegalArgumentException("该日期已被预订，请选择其他日期");
        }

        // 第二道防线（最后底线）：日历占用表检查
        try {
            availabilityService.createAvailabilityRecords(homestayId, order.getCheckInDate(), order.getCheckOutDate());
            
            boolean calendarConflict = availabilityService.hasOverlappingBooking(
                    homestayId, order.getCheckInDate(), order.getCheckOutDate());
            
            if (calendarConflict) {
                log.warn("订单创建失败：日历占用表检测到日期冲突 (homestayId={}, checkIn={}, checkOut={})",
                        homestayId, order.getCheckInDate(), order.getCheckOutDate());
                throw new IllegalArgumentException("该日期已被预订，请选择其他日期");
            }
            
            // 标记日历为已预订
            availabilityService.markAsBooked(order);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            log.warn("日历占用表操作失败，继续使用数据库检查结果: {}", e.getMessage());
        }

        // 设置幂等性 key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            order.setIdempotencyKey(idempotencyKey);
        }

        Order savedOrder = orderRepository.save(order);
        
        log.info("订单创建成功并更新日历占用表: orderId={}, homestayId={}", 
                savedOrder.getId(), homestayId);
        
        return savedOrder;
    }
}
