package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 预订冲突检测服务
 * 提供多层次的并发控制机制，防止重复预订
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingConflictService {

    private final OrderRepository orderRepository;

    // 使用ConcurrentHashMap存储每个房源的锁，防止同一房源的并发预订
    private final ConcurrentHashMap<Long, ReentrantLock> homestayLocks = new ConcurrentHashMap<>();

    /**
     * 获取房源专用锁
     * 
     * @param homestayId 房源ID
     * @return 锁对象
     */
    private ReentrantLock getHomestayLock(Long homestayId) {
        return homestayLocks.computeIfAbsent(homestayId, k -> new ReentrantLock(true));
    }

    /**
     * 检查并防止日期冲突的预订
     * 
     * @param homestayId   房源ID
     * @param checkInDate  入住日期
     * @param checkOutDate 退房日期
     * @return 是否存在冲突
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean checkAndPreventConflict(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        ReentrantLock lock = getHomestayLock(homestayId);

        try {
            // 获取房源专用锁，确保同一房源的预订请求串行处理
            lock.lock();
            log.debug("获取房源 {} 的并发锁成功", homestayId);

            // 使用数据库悲观锁再次检查
            boolean hasConflict = orderRepository.existsOverlappingBookingWithLock(
                    homestayId, checkInDate, checkOutDate);

            if (hasConflict) {
                log.warn("房源 {} 在日期 {} 至 {} 存在预订冲突", homestayId, checkInDate, checkOutDate);
                return true;
            }

            log.info("房源 {} 在日期 {} 至 {} 可以预订", homestayId, checkInDate, checkOutDate);
            return false;

        } finally {
            lock.unlock();
            log.debug("释放房源 {} 的并发锁", homestayId);
        }
    }

    /**
     * 安全地创建订单（在锁保护下）
     * 
     * @param order 订单对象
     * @return 保存的订单
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order safeCreateOrder(Order order) {
        Long homestayId = order.getHomestay().getId();
        ReentrantLock lock = getHomestayLock(homestayId);

        try {
            lock.lock();
            log.debug("在锁保护下创建订单，房源ID: {}", homestayId);

            // 最后一次检查，确保在锁保护期间没有其他订单被创建
            boolean hasConflict = orderRepository.existsOverlappingBookingWithLock(
                    homestayId, order.getCheckInDate(), order.getCheckOutDate());

            if (hasConflict) {
                throw new IllegalArgumentException("所选日期已被预订，请选择其他日期");
            }

            return orderRepository.save(order);

        } finally {
            lock.unlock();
            log.debug("订单创建完成，释放房源 {} 的锁", homestayId);
        }
    }

    /**
     * 清理不再使用的锁（可选的维护方法）
     */
    public void cleanupUnusedLocks() {
        homestayLocks.entrySet().removeIf(entry -> {
            ReentrantLock lock = entry.getValue();
            return !lock.hasQueuedThreads() && !lock.isLocked();
        });
        log.debug("清理未使用的房源锁，当前锁数量: {}", homestayLocks.size());
    }
}