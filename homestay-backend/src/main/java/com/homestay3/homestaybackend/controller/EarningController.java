package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.EarningDTO;
import com.homestay3.homestaybackend.dto.EarningsSummaryDTO;
import com.homestay3.homestaybackend.dto.EarningsTrendDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Earning;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.EarningRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.EarningService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/host/earnings")
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
    allowedHeaders = "*", 
    methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
        RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH
    },
    exposedHeaders = {
        "Content-Type", "Content-Length", "Authorization",
        "Access-Control-Allow-Origin", "Access-Control-Allow-Headers", 
        "Access-Control-Allow-Methods", "Access-Control-Allow-Credentials"
    },
    allowCredentials = "true",
    maxAge = 3600
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOST', 'LANDLORD', 'ADMIN')")
public class EarningController {
    
    private static final Logger log = LoggerFactory.getLogger(EarningController.class);
    private final EarningService earningService;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final OrderRepository orderRepository;
    private final EarningRepository earningRepository;
    
    @GetMapping("/detail")
    public ResponseEntity<?> getEarningsDetail(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long homestayId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("收益明细请求 - 用户: {}, 页码: {}, 大小: {}, 开始日期: {}, 结束日期: {}, 房源ID: {}, 权限: {}",
                    username, page, size, startDate, endDate, homestayId, 
                    authentication.getAuthorities());
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<EarningDTO> earnings = earningService.getEarningsDetail(username, startDate, endDate, homestayId, pageable);
            
            log.info("收益明细返回 - 数据条数: {}, 总页数: {}, 总条数: {}",
                    earnings.getContent().size(), earnings.getTotalPages(), earnings.getTotalElements());
            
            return ResponseEntity.ok(earnings);
        } catch (Exception e) {
            log.error("获取收益明细失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/summary")
    public ResponseEntity<?> getEarningsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long homestayId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("收益概要请求 - 用户: {}, 开始日期: {}, 结束日期: {}, 房源ID: {}, 权限: {}",
                    username, startDate, endDate, homestayId, 
                    authentication.getAuthorities());
            
            EarningsSummaryDTO summary = earningService.getEarningsSummary(username, startDate, endDate, homestayId);
            
            log.info("收益概要返回 - 总收益: {}, 总订单数: {}, 平均收益: {}",
                    summary.getTotalEarnings(), summary.getTotalOrders(), summary.getAveragePerOrder());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("获取收益概要失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/trend")
    public ResponseEntity<?> getEarningsTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String type,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("收益趋势请求 - 用户: {}, 开始日期: {}, 结束日期: {}, 类型: {}, 权限: {}",
                    username, startDate, endDate, type, 
                    authentication.getAuthorities());
            
            EarningsTrendDTO trend = earningService.getEarningsTrend(username, startDate, endDate, type);
            
            log.info("收益趋势返回 - 标签数量: {}, 数据点数量: {}",
                    trend.getLabels().size(), trend.getValues().size());
            
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("获取收益趋势失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyEarnings(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("月度收益请求 - 用户: {}, 权限: {}", username, authentication.getAuthorities());
            
            BigDecimal monthlyEarnings = earningService.getMonthlyEarnings(username);
            
            log.info("月度收益返回 - 金额: {}", monthlyEarnings);
            
            return ResponseEntity.ok(monthlyEarnings);
        } catch (Exception e) {
            log.error("获取月度收益失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取待结算的收益总额
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingEarnings(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("待结算收益请求 - 用户: {}, 权限: {}", username, authentication.getAuthorities());
            
            // 获取待结算收益
            BigDecimal pendingEarnings = earningService.getPendingEarnings(username);
            
            log.info("待结算收益返回 - 金额: {}", pendingEarnings);
            
            return ResponseEntity.ok(pendingEarnings);
        } catch (Exception e) {
            log.error("获取待结算收益失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 手动触发结算指定房东的可结算收益
     */
    @PostMapping("/settle")
    public ResponseEntity<?> settleEarnings(Authentication authentication) {
        String username = authentication.getName();
        log.info("收到用户 {} 手动触发收益结算的请求", username);
        try {
            int settledCount = earningService.settleHostEarnings(username);
            if (settledCount > 0) {
                log.info("成功为用户 {} 结算了 {} 条收益记录", username, settledCount);
                return ResponseEntity.ok(Map.of(
                    "message", "成功结算 " + settledCount + " 条收益记录。",
                    "settledCount", settledCount
                ));
            } else {
                log.info("用户 {} 没有需要结算的收益记录", username);
                 return ResponseEntity.ok(Map.of(
                    "message", "没有需要结算的收益记录。",
                    "settledCount", 0
                ));
            }
        } catch (ResourceNotFoundException e) {
             log.warn("触发结算失败，用户 {} 不存在", username);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
             log.error("为用户 {} 结算收益时发生意外错误: {}", username, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(Map.of("error", "结算过程中发生错误，请稍后重试或联系管理员"));
        }
    }
    
    /**
     * 初始化测试数据 - 仅供开发环境使用
     */
    @PostMapping("/init-test-data")
    public ResponseEntity<?> initTestData(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("初始化测试收益数据请求 - 用户: {}, 权限: {}", username, authentication.getAuthorities());
            
            // 查找用户
            User host = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            log.info("找到用户: ID={}, 用户名={}, 角色={}", host.getId(), host.getUsername(), host.getRole());
            
            // 查找当前用户的房源和订单
            List<Homestay> homestays = homestayRepository.findByOwner(host);
            if (homestays.isEmpty()) {
                log.warn("用户 {} 没有房源", username);
                return ResponseEntity.badRequest().body(Map.of("error", "用户没有房源，请先创建房源"));
            }
            log.info("找到房源数量: {}", homestays.size());
            
            // 使用findByOwner方法替代findByHomestayOwner方法，并添加分页参数
            Page<Order> orderPage = orderRepository.findByOwner(host, PageRequest.of(0, 100));
            List<Order> orders = orderPage.getContent();
            if (orders.isEmpty()) {
                log.warn("用户 {} 没有订单", username);
                return ResponseEntity.badRequest().body(Map.of("error", "用户没有订单，请先创建订单"));
            }
            log.info("找到订单数量: {}", orders.size());
            
            // 创建测试收益数据
            List<Earning> testEarnings = new ArrayList<>();
            
            // 今年的每月数据
            LocalDate date = LocalDate.now().withDayOfMonth(1);
            for (int i = 5; i >= 0; i--) {
                LocalDate month = date.minusMonths(i);
                log.debug("为月份 {} 创建测试数据", month);
                for (int j = 0; j < 3; j++) {
                    int randomIndex = (int)(Math.random() * homestays.size());
                    Homestay homestay = homestays.get(randomIndex);
                    
                    int randomOrderIndex = (int)(Math.random() * orders.size());
                    Order order = orders.get(randomOrderIndex);
                    
                    // 随机金额
                    BigDecimal amount = BigDecimal.valueOf(Math.round(500 + Math.random() * 1500));
                    
                    // 随机日期
                    LocalDate checkIn = month.withDayOfMonth((int)(Math.random() * 25) + 1);
                    LocalDate checkOut = checkIn.plusDays((int)(Math.random() * 5) + 1);
                    
                    // 随机状态
                    String[] statuses = {"SETTLED", "PENDING"};
                    String status = statuses[(int)(Math.random() * statuses.length)];
                    
                    Earning earning = Earning.builder()
                            .host(host)
                            .homestay(homestay)
                            .order(order)
                            .amount(amount)
                            .checkInDate(checkIn)
                            .checkOutDate(checkOut)
                            .nights((int)checkIn.until(checkOut).getDays())
                            .status(status)
                            .isTest(true)
                            .build();
                    
                    testEarnings.add(earning);
                }
            }
            
            // 保存测试数据
            earningRepository.saveAll(testEarnings);
            
            log.info("成功初始化测试收益数据，条数: {}", testEarnings.size());
            log.info("示例数据: 入住日期={}, 金额={}, 状态={}", 
                    testEarnings.get(0).getCheckInDate(), 
                    testEarnings.get(0).getAmount(),
                    testEarnings.get(0).getStatus());
            
            return ResponseEntity.ok(Map.of(
                "message", "成功初始化测试收益数据",
                "count", testEarnings.size()
            ));
        } catch (Exception e) {
            log.error("初始化测试收益数据失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 