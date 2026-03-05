package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "lock:homestay:%d:date:%s";

    /**
     * 获取日期范围内的所有分布式锁
     * 
     * @param homestayId   房源ID
     * @param checkInDate  入住日期
     * @param checkOutDate 退房日期
     * @return MultiLock 对象
     */
    private RLock getDateRangeLock(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<RLock> locks = new ArrayList<>();
        // 预订日期包含入住日，但不包含退房日（退房日当天其他人可以入住）
        for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
            String lockKey = String.format(LOCK_KEY_PREFIX, homestayId, date.toString());
            locks.add(redissonClient.getLock(lockKey));
        }
        return redissonClient.getMultiLock(locks.toArray(new RLock[0]));
    }

    /**
     * 检查并防止日期冲突的预订
     * 使用 Redis 分布式锁确保细粒度并发控制
     * 
     * @param homestayId   房源ID
     * @param checkInDate  入住日期
     * @param checkOutDate 退房日期
     * @return 是否存在冲突
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean checkAndPreventConflict(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        RLock multiLock = getDateRangeLock(homestayId, checkInDate, checkOutDate);

        try {
            // 尝试获取锁，等待时间 5s，租赁时间 30s（带看门狗自动续期）
            boolean isLocked = multiLock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("无法获取房源 {} 在日期 {} 至 {} 的并发锁", homestayId, checkInDate, checkOutDate);
                throw new RuntimeException("预订请求繁忙，请稍后重试");
            }

            log.debug("获取房源 {} 日期范围分布式锁成功", homestayId);

            // 数据库层面的重叠检查
            boolean hasConflict = orderRepository.existsOverlappingBooking(
                    homestayId, checkInDate, checkOutDate);

            if (hasConflict) {
                log.warn("房源 {} 在日期 {} 至 {} 存在预订冲突", homestayId, checkInDate, checkOutDate);
                // 如果发现冲突，提前释放锁
                multiLock.unlock();
                return true;
            }

            log.info("房源 {} 在日期 {} 至 {} 可以预订", homestayId, checkInDate, checkOutDate);
            // 注意：这里由于锁是在 try 块外手动释放或依赖 finally 释放。
            // 实际上，如果 checkAndPreventConflict 返回 false，锁应该被持有直到订单创建完成。
            // 但原逻辑中 checkAndPreventConflict 和 safeCreateOrder 是分开调用的。
            // 为了安全，我们让 checkAndPreventConflict 仅做检查，并立即释放锁。
            // 真正的“原子性”应由 safeCreateOrder 来保证。
            multiLock.unlock();
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("预订中断，请重试");
        }
    }

    /**
     * 安全地创建订单（在 Redis 分布式锁保护下）
     * 
     * @param order 订单对象
     * @return 保存的订单
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order safeCreateOrder(Order order) {
        Long homestayId = order.getHomestay().getId();
        RLock multiLock = getDateRangeLock(homestayId, order.getCheckInDate(), order.getCheckOutDate());

        try {
            // 获取分布式锁
            boolean isLocked = multiLock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("预订系统繁忙，未能获得锁定，请稍后重试");
            }

            log.debug("在 Redis 锁保护下创建订单，房源ID: {}", homestayId);

            // 最终冲突检查
            boolean hasConflict = orderRepository.existsOverlappingBooking(
                    homestayId, order.getCheckInDate(), order.getCheckOutDate());

            if (hasConflict) {
                throw new IllegalArgumentException("所选日期已被预订，请选择其他日期");
            }

            return orderRepository.save(order);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("订单创建中断，请重试");
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
                log.debug("订单创建流程结束，释放房源 {} 日期分布式锁", homestayId);
            }
        }
    }
}
