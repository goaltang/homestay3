package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.EarningDTO;
import com.homestay3.homestaybackend.dto.EarningsSummaryDTO;
import com.homestay3.homestaybackend.dto.EarningsTrendDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Earning;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.User;
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
    public EarningDTO createEarningFromOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 只有已完成的订单才能创建收益记录
        if (!order.getStatus().equals(OrderStatus.COMPLETED.name())) {
            throw new IllegalArgumentException("只有已完成的订单才能创建收益记录");
        }
        
        // 检查是否已经存在该订单的收益记录
        Specification<Earning> spec = (root, query, cb) -> 
            cb.equal(root.get("order").get("id"), orderId);
        
        boolean earningExists = earningRepository.findAll(spec).size() > 0;
        if (earningExists) {
            throw new IllegalArgumentException("该订单的收益记录已存在");
        }
        
        // 创建收益记录
        Earning earning = Earning.builder()
                .host(order.getHomestay().getOwner())
                .homestay(order.getHomestay())
                .order(order)
                .amount(order.getTotalAmount())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .status("SETTLED")
                .build();
        
        Earning savedEarning = earningRepository.save(earning);
        return convertToDTO(savedEarning);
    }
    
    // 工具方法：将 Earning 实体转换为 EarningDTO
    private EarningDTO convertToDTO(Earning earning) {
        return EarningDTO.builder()
                .id(earning.getId())
                .orderNumber(earning.getOrder().getOrderNumber())
                .homestayId(earning.getHomestay().getId())
                .homestayTitle(earning.getHomestay().getTitle())
                .guestName(earning.getOrder().getGuest().getUsername())
                .checkInDate(earning.getCheckInDate())
                .checkOutDate(earning.getCheckOutDate())
                .nights(earning.getNights())
                .amount(earning.getAmount())
                .status(earning.getStatus())
                .createTime(earning.getCreatedAt())
                .build();
    }
} 