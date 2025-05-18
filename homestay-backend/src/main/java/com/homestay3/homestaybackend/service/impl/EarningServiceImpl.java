package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.EarningDTO;
import com.homestay3.homestaybackend.dto.EarningsSummaryDTO;
import com.homestay3.homestaybackend.dto.EarningsTrendDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Earning;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.EarningRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.service.EarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarningServiceImpl implements EarningService {
    
    private final EarningRepository earningRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final HomestayRepository homestayRepository;
    
    @Value("${app.earnings.host-share-rate:0.8}")
    private BigDecimal hostShareRate;
    
    @Override
    @Transactional(readOnly = true)
    public Page<EarningDTO> getEarningsDetail(String hostUsername, LocalDate startDate, LocalDate endDate, 
                                              Long homestayId, Pageable pageable) {
        try {
            User host = userRepository.findByUsername(hostUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
            log.debug("查询收益明细 - 用户:{}, 开始日期:{}, 结束日期:{}, 房源ID:{}", 
                    hostUsername, startDate, endDate, homestayId);
            
            Specification<Earning> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // 指定房东
                predicates.add(cb.equal(root.get("host"), host));
                
                // 日期范围
                if (startDate != null && endDate != null) {
                    predicates.add(cb.between(root.get("checkInDate"), startDate, endDate));
                }
                
                // 指定房源
                if (homestayId != null) {
                    predicates.add(cb.equal(root.get("homestay").get("id"), homestayId));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            
            Page<Earning> earningsPage = earningRepository.findAll(spec, pageable);
            log.debug("查询结果 - 总条数:{}, 总页数:{}", earningsPage.getTotalElements(), earningsPage.getTotalPages());
            
            return earningsPage.map(this::convertToDTO);
        } catch (Exception e) {
            log.error("获取收益明细异常: {}", e.getMessage(), e);
            return Page.empty(pageable);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public EarningsSummaryDTO getEarningsSummary(String hostUsername, LocalDate startDate, LocalDate endDate, 
                                                Long homestayId) {
        try {
            User host = userRepository.findByUsername(hostUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
            // 创建查询条件
            Specification<Earning> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // 指定房东
                predicates.add(cb.equal(root.get("host"), host));
                
                // 日期范围
                if (startDate != null && endDate != null) {
                    predicates.add(cb.between(root.get("checkInDate"), startDate, endDate));
                }
                
                // 指定房源
                if (homestayId != null) {
                    predicates.add(cb.equal(root.get("homestay").get("id"), homestayId));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            
            // 计算总收入
            BigDecimal totalEarnings = BigDecimal.ZERO;
            List<Earning> earnings = earningRepository.findAll(spec);
            for (Earning earning : earnings) {
                totalEarnings = totalEarnings.add(earning.getAmount());
            }
            
            // 计算订单数
            long totalOrders = earnings.size();
            
            // 计算平均每单收入
            BigDecimal averagePerOrder = totalOrders > 0 
                    ? totalEarnings.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            return EarningsSummaryDTO.builder()
                    .totalEarnings(totalEarnings)
                    .totalOrders(totalOrders)
                    .averagePerOrder(averagePerOrder)
                    .build();
        } catch (Exception e) {
            // 出现异常时返回空数据而不是模拟数据
            return EarningsSummaryDTO.builder()
                    .totalEarnings(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .averagePerOrder(BigDecimal.ZERO)
                    .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public EarningsTrendDTO getEarningsTrend(String hostUsername, LocalDate startDate, LocalDate endDate, 
                                           String type) {
        try {
            User host = userRepository.findByUsername(hostUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(6);
            }
            
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            List<Object[]> trendData;
            if ("monthly".equals(type)) {
                trendData = earningRepository.getMonthlyEarningsTrend(host, startDate, endDate);
            } else {
                trendData = earningRepository.getDailyEarningsTrend(host, startDate, endDate);
            }
            
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            
            for (Object[] data : trendData) {
                labels.add(String.valueOf(data[0]));
                
                BigDecimal amount = (BigDecimal) data[1];
                values.add(amount != null ? amount.doubleValue() : 0.0);
            }
            
            return EarningsTrendDTO.builder()
                    .labels(labels)
                    .values(values)
                    .build();
        } catch (Exception e) {
            // 出现异常时返回空数据而不是模拟数据
            return EarningsTrendDTO.builder()
                    .labels(new ArrayList<>())
                    .values(new ArrayList<>())
                    .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyEarnings(String hostUsername) {
        try {
            User host = userRepository.findByUsername(hostUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
            LocalDate now = LocalDate.now();
            
            BigDecimal monthlyEarnings = earningRepository.sumMonthlyEarningsByHost(
                    host, now.getYear(), now.getMonthValue());
            
            return monthlyEarnings != null ? monthlyEarnings : BigDecimal.ZERO;
        } catch (Exception e) {
            // 出现异常时返回零值而不是模拟数据
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPendingEarnings(String hostUsername) {
        try {
            User host = userRepository.findByUsername(hostUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
            // 创建查询条件：指定房东且状态为PENDING
            Specification<Earning> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("host"), host));
                predicates.add(cb.equal(root.get("status"), "PENDING"));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            
            // 查询所有符合条件的收益记录
            List<Earning> pendingEarnings = earningRepository.findAll(spec);
            
            // 计算总和
            BigDecimal totalPending = BigDecimal.ZERO;
            for (Earning earning : pendingEarnings) {
                totalPending = totalPending.add(earning.getAmount());
            }
            
            return totalPending;
        } catch (Exception e) {
            // 出现异常时返回零值而不是模拟数据
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    @Transactional
    public EarningDTO generatePendingEarningForOrder(Long orderId) {
        log.info("开始为订单 ID: {} 生成待结算收益记录...", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
        log.debug("找到订单: {}, 状态: {}, 金额: {}", order.getOrderNumber(), order.getStatus(), order.getTotalAmount());

        log.debug("检查订单 {} 是否已存在收益记录...", order.getOrderNumber());
        boolean exists = earningRepository.existsByOrder(order);
        log.debug("检查结果，订单 {} {} 存在收益记录。", order.getOrderNumber(), exists ? "已" : "不");

        if (exists) {
             log.warn("订单 {} ({}) 已存在收益记录，跳过创建。", order.getOrderNumber(), orderId);
             Earning existingEarning = earningRepository.findByOrder(order)
                     .orElse(null);
             if (existingEarning != null) {
                 log.debug("找到已存在的收益记录 ID: {}", existingEarning.getId());
                 return convertToDTO(existingEarning);
             } else {
                 // This case should ideally not happen if exists is true, but log it if it does
                 log.error("数据不一致：订单 {} ({}) 标记为存在收益记录，但无法查询到具体记录。", order.getOrderNumber(), orderId);
                 return null; // Indicate failure or inconsistency
             }
        }

        log.debug("计算收益金额，房东分成比例: {}", hostShareRate);
        BigDecimal earningAmount = order.getTotalAmount().multiply(hostShareRate)
                .setScale(2, RoundingMode.HALF_UP);
        log.info("计算得出订单 {} 的收益金额为: {}", order.getOrderNumber(), earningAmount);

        // Check if amount is zero or negative, though saving should still work
        if (earningAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("订单 {} 计算出的收益金额为零或负数: {}，仍将尝试创建记录。", order.getOrderNumber(), earningAmount);
        }

        Earning earning = Earning.builder()
                .host(order.getHomestay().getOwner())
                .homestay(order.getHomestay())
                .order(order)
                .amount(earningAmount)
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .status("PENDING") // 初始状态为 PENDING
                // isTest 字段默认应为 false 或 null，除非是测试数据
                .build();
        log.debug("准备保存的 Earning 对象: {}", earning);

        try {
            Earning savedEarning = earningRepository.save(earning);
            log.info("成功为订单 {} 创建待结算收益记录 ID: {}",
                    order.getOrderNumber(), savedEarning.getId());
            return convertToDTO(savedEarning);
        } catch (Exception e) {
            // Log the full stack trace for better debugging
            log.error("保存订单 {} 的收益记录时发生严重错误: {}", order.getOrderNumber(), e.getMessage(), e);
             // Consider re-throwing a specific exception or returning an error indicator
             // For now, returning null as before, but the detailed log is crucial
            return null;
        }
    }
    
    @Override
    @Transactional // 添加事务注解
    public int settleHostEarnings(String hostUsername) {
        log.info("开始为用户 {} 结算收益...", hostUsername);
        User host = userRepository.findByUsername(hostUsername)
                .orElseThrow(() -> {
                     log.error("结算收益失败：找不到用户 {}", hostUsername);
                     return new ResourceNotFoundException("用户不存在: " + hostUsername);
                 });

        // 移除日期检查: LocalDate today = LocalDate.now();
        log.debug("查找用户 {} 所有状态为 PENDING 的收益记录", host.getUsername());

        // 使用 Specification 查询，不再检查退房日期
        Specification<Earning> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("host"), host));
            predicates.add(cb.equal(root.get("status"), "PENDING"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Earning> eligibleEarnings = earningRepository.findAll(spec);

        if (eligibleEarnings.isEmpty()) {
            log.info("用户 {} 没有需要结算的收益记录。", hostUsername);
            return 0;
        }

        log.info("找到 {} 条符合结算条件的收益记录，准备更新状态为 SETTLED...", eligibleEarnings.size());

        for (Earning earning : eligibleEarnings) {
            earning.setStatus("SETTLED");
            // updatedAt 字段应该由 @UpdateTimestamp 自动更新
        }

        try {
            earningRepository.saveAll(eligibleEarnings);
            log.info("成功结算了 {} 条用户 {} 的收益记录。", eligibleEarnings.size(), hostUsername);
            return eligibleEarnings.size();
        } catch (Exception e) {
            log.error("批量保存结算后的收益记录时出错 (用户: {}): {}", hostUsername, e.getMessage(), e);
            // 这里可以考虑抛出异常让 Controller 层捕获并返回错误信息
            // 或者根据业务需求决定是否部分成功也算成功
            throw new RuntimeException("结算收益时数据库更新失败", e); // 抛出运行时异常
        }
    }
    
    private EarningDTO convertToDTO(Earning earning) {
        if (earning == null) {
            return null;
        }
        // Safely access guest information
        User guest = earning.getOrder() != null ? earning.getOrder().getGuest() : null;
        String guestName = guest != null ? (guest.getNickname() != null ? guest.getNickname() : guest.getUsername()) : "未知"; // Use nickname if available, else username
        
        return EarningDTO.builder()
                .id(earning.getId())
                .hostId(earning.getHost().getId())
                .homestayId(earning.getHomestay().getId())
                .homestayTitle(earning.getHomestay().getTitle())
                .orderId(earning.getOrder().getId())
                .orderNumber(earning.getOrder().getOrderNumber())
                .guestName(guestName) // Populate guestName
                .amount(earning.getAmount())
                .checkInDate(earning.getCheckInDate())
                .checkOutDate(earning.getCheckOutDate())
                .nights(earning.getNights())
                .status(earning.getStatus())
                .createdAt(earning.getCreatedAt())
                .build();
    }
} 