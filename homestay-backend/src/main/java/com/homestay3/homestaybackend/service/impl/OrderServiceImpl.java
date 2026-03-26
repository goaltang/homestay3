package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.dto.PriceCalculationRequest;
import com.homestay3.homestaybackend.dto.PriceCalculationResponse;
import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.dto.EarningDTO;
import com.homestay3.homestaybackend.dto.refund.RefundRequest;
import com.homestay3.homestaybackend.dto.refund.RefundResponse;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.Review;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.RefundType;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.OrderNotificationService;
import com.homestay3.homestaybackend.service.SystemConfigService;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.service.OrderService;
import com.homestay3.homestaybackend.service.EarningService;
import com.homestay3.homestaybackend.service.BookingConflictService;
import com.homestay3.homestaybackend.service.PaymentService;
import com.homestay3.homestaybackend.service.PaymentProcessingService;
import com.homestay3.homestaybackend.service.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final NotificationService notificationService;
    private final OrderNotificationService orderNotificationService;
    private final EarningService earningService;
    private final ReviewRepository reviewRepository;
    private final BookingConflictService bookingConflictService;
    private final PaymentProcessingService paymentProcessingService;
    private final OrderLifecycleService orderLifecycleService;
    private final ObjectProvider<SystemConfigService> systemConfigServiceProvider;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            HomestayRepository homestayRepository,
                            NotificationService notificationService,
                            OrderNotificationService orderNotificationService,
                            EarningService earningService,
                            ReviewRepository reviewRepository,
                            BookingConflictService bookingConflictService,
                            PaymentProcessingService paymentProcessingService,
                            OrderLifecycleService orderLifecycleService,
                            ObjectProvider<SystemConfigService> systemConfigServiceProvider) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.notificationService = notificationService;
        this.orderNotificationService = orderNotificationService;
        this.earningService = earningService;
        this.reviewRepository = reviewRepository;
        this.bookingConflictService = bookingConflictService;
        this.paymentProcessingService = paymentProcessingService;
        this.orderLifecycleService = orderLifecycleService;
        this.systemConfigServiceProvider = systemConfigServiceProvider;
    }
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.createOrder(orderDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.getOrderById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.getOrderByOrderNumber(orderNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getMyOrders(Map<String, String> params, Pageable pageable) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.getMyOrders(params, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOwnerOrders(String ownerUsername, Map<String, String> params, Pageable pageable) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.getOwnerOrders(ownerUsername, params, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPendingOrderCount(String ownerUsername) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.getPendingOrderCount(ownerUsername);
    }

    @Override
    @Transactional
    public OrderDTO confirmOrder(Long id) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.confirmOrder(id);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.updateOrderStatus(id, status);
    }

    /**
     * 验证订单状态流转是否合法
     * 
     * @param currentStatus 当前状态
     * @param targetStatus  目标状态
     * @return 如果状态流转合法则返回true
     */
    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        // 定义状态流转规则
        Map<OrderStatus, List<OrderStatus>> transitions = new HashMap<>();

        // 待确认状态可以转换为：已确认、已取消（各种取消状态）、已拒绝
        transitions.put(OrderStatus.PENDING, Arrays.asList(
                OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED,
                OrderStatus.CANCELLED_BY_USER,
                OrderStatus.CANCELLED_BY_HOST,
                OrderStatus.CANCELLED_SYSTEM,
                OrderStatus.REJECTED));

        // 已确认状态可以转换为：支付中、已取消、已支付
        transitions.put(OrderStatus.CONFIRMED, Arrays.asList(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.CANCELLED,
                OrderStatus.CANCELLED_BY_USER,
                OrderStatus.CANCELLED_BY_HOST,
                OrderStatus.CANCELLED_SYSTEM,
                OrderStatus.PAID // 直接支付也可以
        ));

        // 支付中状态可以转换为：已支付、支付失败、用户取消、系统取消
        transitions.put(OrderStatus.PAYMENT_PENDING, Arrays.asList(
                OrderStatus.PAID,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.CANCELLED_BY_USER,
                OrderStatus.CANCELLED, // 系统取消
                OrderStatus.CANCELLED_SYSTEM // 系统自动取消（用于超时处理）
        ));

        // 支付失败状态可以转换为：支付中、已取消、用户取消、系统取消
        transitions.put(OrderStatus.PAYMENT_FAILED, Arrays.asList(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.CANCELLED,
                OrderStatus.CANCELLED_BY_USER,
                OrderStatus.CANCELLED_SYSTEM // 系统自动取消
        ));

        // 已支付状态可以转换为：待入住、退款中、各种取消状态、已入住
        transitions.put(OrderStatus.PAID, Arrays.asList(
                OrderStatus.READY_FOR_CHECKIN,
                OrderStatus.REFUND_PENDING,
                OrderStatus.CANCELLED_BY_HOST,
                OrderStatus.CANCELLED_BY_USER, // 用户取消
                OrderStatus.CANCELLED_SYSTEM, // 系统取消
                OrderStatus.CANCELLED, // 通用取消
                OrderStatus.CHECKED_IN // 直接入住也可以
        ));

        // 待入住状态可以转换为：已入住、退款中、各种取消状态
        transitions.put(OrderStatus.READY_FOR_CHECKIN, Arrays.asList(
                OrderStatus.CHECKED_IN,
                OrderStatus.REFUND_PENDING,
                OrderStatus.CANCELLED_BY_HOST,
                OrderStatus.CANCELLED_BY_USER, // 用户取消
                OrderStatus.CANCELLED_SYSTEM, // 系统取消
                OrderStatus.CANCELLED // 通用取消
        ));

        // 已入住状态只能转换为：已完成
        transitions.put(OrderStatus.CHECKED_IN, List.of(OrderStatus.COMPLETED));

        // 已完成状态不能转换为任何状态
        transitions.put(OrderStatus.COMPLETED, Collections.emptyList());

        // 已取消（各种取消状态）不能转换为任何状态，除了房东取消可能需要触发退款
        transitions.put(OrderStatus.CANCELLED, Collections.emptyList());
        transitions.put(OrderStatus.CANCELLED_BY_USER, Collections.emptyList());
        transitions.put(OrderStatus.CANCELLED_BY_HOST, List.of(OrderStatus.REFUND_PENDING));
        transitions.put(OrderStatus.CANCELLED_SYSTEM, Collections.emptyList());

        // 退款相关状态
        transitions.put(OrderStatus.REFUND_PENDING, Arrays.asList(
                OrderStatus.REFUNDED,
                OrderStatus.REFUND_FAILED));
        transitions.put(OrderStatus.REFUNDED, Collections.emptyList());
        transitions.put(OrderStatus.REFUND_FAILED, List.of(OrderStatus.REFUND_PENDING));

        // 拒绝状态不能转换为任何状态
        transitions.put(OrderStatus.REJECTED, Collections.emptyList());

        // 检查当前状态是否有对应的流转规则
        if (!transitions.containsKey(currentStatus)) {
            return false;
        }

        // 检查目标状态是否在允许的流转列表中
        return transitions.get(currentStatus).contains(targetStatus);
    }

    /**
     * 计算退款金额
     * 根据房源取消政策（cancelPolicyType）和距离入住还剩多少小时来决定退款比例：
     * 政策1（宽松）：24h前全额退；24h内扣首晚
     * 政策2（普通，默认）：48h前全额退；24-48h退50%；24h内扣首晚
     * 政策3（严格）：72h前全额退；72h内退50%
     */
    private BigDecimal calculateRefundAmount(Order order) {
        if (order.getCheckInDate() == null || order.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInTime = order.getCheckInDate().atTime(14, 0); // 假设14:00入住
        long hoursBetween = java.time.Duration.between(now, checkInTime).toHours();

        // 读取房源取消政策类型（1=宽松，2=普通，3=严格），默认为2
        int policyType = 2;
        if (order.getHomestay() != null && order.getHomestay().getCancelPolicyType() != null) {
            policyType = order.getHomestay().getCancelPolicyType();
        }

        BigDecimal refundAmt;
        String policyNote;

        if (policyType == 1) {
            // 宽松政策：入住24小时前取消 → 全额退款；24小时内 → 扣除首晚
            if (hoursBetween >= 24) {
                refundAmt = order.getTotalAmount();
                policyNote = "退款测算（宽松政策）: 距离入住大于24小时，提供全额退款。";
            } else {
                int nights = order.getNights() != null ? order.getNights() : 1;
                if (nights <= 1) {
                    refundAmt = BigDecimal.ZERO;
                    policyNote = "退款测算（宽松政策）: 距离入住不足24小时（仅1晚），不予退款。";
                } else {
                    BigDecimal perNight = order.getTotalAmount().divide(new BigDecimal(nights), 2, java.math.RoundingMode.HALF_UP);
                    refundAmt = order.getTotalAmount().subtract(perNight);
                    if (refundAmt.compareTo(BigDecimal.ZERO) < 0) refundAmt = BigDecimal.ZERO;
                    policyNote = "退款测算（宽松政策）: 距离入住不足24小时，扣除首晚房费。";
                }
            }
        } else if (policyType == 3) {
            // 严格政策：入住72小时前取消 → 全额退款；72小时内 → 退款50%
            if (hoursBetween >= 72) {
                refundAmt = order.getTotalAmount();
                policyNote = "退款测算（严格政策）: 距离入住大于72小时，提供全额退款。";
            } else {
                refundAmt = order.getTotalAmount().multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_UP);
                policyNote = "退款测算（严格政策）: 距离入住不足72小时，退款50%。";
            }
        } else {
            // 普通政策（默认policyType=2）：48h前全额退；24-48h退50%；24h内扣首晚
            if (hoursBetween >= 48) {
                refundAmt = order.getTotalAmount();
                policyNote = "退款测算（普通政策）: 距离入住大于48小时，提供全额退款。";
            } else if (hoursBetween >= 24) {
                refundAmt = order.getTotalAmount().multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_UP);
                policyNote = "退款测算（普通政策）: 距离入住24-48小时，退款50%。";
            } else {
                int nights = order.getNights() != null ? order.getNights() : 1;
                if (nights <= 1) {
                    refundAmt = BigDecimal.ZERO;
                    policyNote = "退款测算（普通政策）: 距离入住不足24小时（仅1晚），不予退款。";
                } else {
                    BigDecimal perNight = order.getTotalAmount().divide(new BigDecimal(nights), 2, java.math.RoundingMode.HALF_UP);
                    refundAmt = order.getTotalAmount().subtract(perNight);
                    if (refundAmt.compareTo(BigDecimal.ZERO) < 0) refundAmt = BigDecimal.ZERO;
                    policyNote = "退款测算（普通政策）: 距离入住不足24小时，扣除首晚房费。";
                }
            }
        }

        order.setRemark((order.getRemark() != null ? order.getRemark() + "\n" : "") + policyNote);
        return refundAmt;
    }

    /**
     * 根据政策类型和距离入住时间，计算退款金额和对应说明（纯查询，不修改 order 备注）
     */
    private Map<String, Object> buildRefundPreviewInfo(Order order) {
        if (order.getCheckInDate() == null || order.getTotalAmount() == null) {
            return Map.of(
                "estimatedRefundAmount", BigDecimal.ZERO,
                "policyDescription", "无法计算退款金额（缺少入住日期或订单金额）"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInTime = order.getCheckInDate().atTime(14, 0);
        // 修复：如果已超过入住时间，hoursBetween会为负数，取绝对值后按"已过入住时间"处理
        long hoursBetween = Math.abs(java.time.Duration.between(now, checkInTime).toHours());

        int policyType = 2;
        if (order.getHomestay() != null && order.getHomestay().getCancelPolicyType() != null) {
            policyType = order.getHomestay().getCancelPolicyType();
        }

        BigDecimal refundAmt;
        String policyDescription;

        if (policyType == 1) {
            if (hoursBetween >= 24) {
                refundAmt = order.getTotalAmount();
                policyDescription = "宽松政策：距离入住超过24小时，可获得全额退款 ¥" + refundAmt;
            } else {
                int nights = order.getNights() != null ? order.getNights() : 1;
                if (nights <= 1) {
                    refundAmt = BigDecimal.ZERO;
                    policyDescription = "宽松政策：距离入住不足24小时（仅1晚），不予退款";
                } else {
                    BigDecimal perNight = order.getTotalAmount().divide(new BigDecimal(nights), 2, java.math.RoundingMode.HALF_UP);
                    refundAmt = order.getTotalAmount().subtract(perNight);
                    if (refundAmt.compareTo(BigDecimal.ZERO) < 0) refundAmt = BigDecimal.ZERO;
                    policyDescription = "宽松政策：距离入住不足24小时，扣除首晚房费，可退 ¥" + refundAmt;
                }
            }
        } else if (policyType == 3) {
            if (hoursBetween >= 72) {
                refundAmt = order.getTotalAmount();
                policyDescription = "严格政策：距离入住超过72小时，可获得全额退款 ¥" + refundAmt;
            } else {
                refundAmt = order.getTotalAmount().multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_UP);
                policyDescription = "严格政策：距离入住不足72小时，退款50%，预计退款 ¥" + refundAmt;
            }
        } else {
            if (hoursBetween >= 48) {
                refundAmt = order.getTotalAmount();
                policyDescription = "普通政策：距离入住超过48小时，可获得全额退款 ¥" + refundAmt;
            } else if (hoursBetween >= 24) {
                refundAmt = order.getTotalAmount().multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_UP);
                policyDescription = "普通政策：距离入住24-48小时，退款50%，预计退款 ¥" + refundAmt;
            } else {
                int nights = order.getNights() != null ? order.getNights() : 1;
                if (nights <= 1) {
                    refundAmt = BigDecimal.ZERO;
                    policyDescription = "普通政策：距离入住不足24小时（仅1晚），不予退款";
                } else {
                    BigDecimal perNight = order.getTotalAmount().divide(new BigDecimal(nights), 2, java.math.RoundingMode.HALF_UP);
                    refundAmt = order.getTotalAmount().subtract(perNight);
                    if (refundAmt.compareTo(BigDecimal.ZERO) < 0) refundAmt = BigDecimal.ZERO;
                    policyDescription = "普通政策：距离入住不足24小时，扣除首晚房费，预计退款 ¥" + refundAmt;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("estimatedRefundAmount", refundAmt);
        result.put("totalAmount", order.getTotalAmount());
        result.put("policyDescription", policyDescription);
        result.put("policyType", policyType);
        result.put("hoursBeforeCheckIn", hoursBetween);
        return result;
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long id) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.cancelOrder(id);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrderWithReason(Long id, String cancelType, String reason) {
        // 委托给OrderLifecycleService处理
        return orderLifecycleService.cancelOrderWithReason(id, cancelType, reason);
    }

    /**
     * 系统级取消订单方法，用于定时任务等不需要用户认证的场景
     * 此方法不检查用户权限，请谨慎使用
     */
    @Override
    @Transactional
    public OrderDTO systemCancelOrder(Long id, String cancelType, String reason) {
        // 委托给OrderLifecycleService处理
        return orderLifecycleService.systemCancelOrder(id, cancelType, reason);
    }

    @Override
    @Transactional
    public OrderDTO payOrder(Long id) {
        // 委托给PaymentProcessingService处理
        return paymentProcessingService.processPayment(id);
    }

    @Override
    @Transactional
    public OrderDTO payOrder(Long id, String paymentMethod) {
        // 委托给PaymentProcessingService处理
        return paymentProcessingService.processPayment(id, paymentMethod);
    }

    @Override
    public OrderDTO createOrderPreview(OrderDTO orderDTO) {
        // 获取房源信息
        Homestay homestay = homestayRepository.findById(orderDTO.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));

        // 获取当前用户信息
        User currentUser = getCurrentUser();

        // 检查日期是否有效
        if (orderDTO.getCheckInDate() == null || orderDTO.getCheckOutDate() == null) {
            throw new IllegalArgumentException("入住和退房日期不能为空");
        }

        if (orderDTO.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }

        if (orderDTO.getCheckOutDate().isBefore(orderDTO.getCheckInDate())) {
            throw new IllegalArgumentException("退房日期不能早于入住日期");
        }

        // 计算住宿天数
        long nights = ChronoUnit.DAYS.between(orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (nights < homestay.getMinNights()) {
            throw new IllegalArgumentException("住宿天数不能少于" + homestay.getMinNights() + "晚");
        }

        // 检查是否有重叠的预订
        boolean hasOverlap = orderRepository.existsOverlappingBooking(
                homestay.getId(), orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (hasOverlap) {
            throw new IllegalArgumentException("所选日期已被预订，请选择其他日期");
        }

        // 计算价格信息
        BigDecimal pricePerNight = homestay.getPrice();
        BigDecimal baseAmount = pricePerNight.multiply(BigDecimal.valueOf(nights));

        // 从系统配置动态读取费用配置
        // 清洁费：固定金额 = 单晚价格 × 配置比例（一次性，不论住多少晚）
        BigDecimal cleaningFee = pricePerNight.multiply(getPricingConfig("pricing.cleaning_fee", "0.1"));
        // 服务费：按订单总价的百分比收取
        BigDecimal serviceFee = baseAmount.multiply(getPricingConfig("pricing.service_fee", "0.15"));

        // 计算总价
        BigDecimal totalAmount = baseAmount.add(cleaningFee).add(serviceFee);

        // 创建预览订单DTO对象
        OrderDTO previewOrderDTO = OrderDTO.builder()
                .homestayId(homestay.getId())
                .homestayTitle(homestay.getTitle())
                .guestId(currentUser.getId())
                .guestName(currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername())
                .guestPhone(orderDTO.getGuestPhone())
                .checkInDate(orderDTO.getCheckInDate())
                .checkOutDate(orderDTO.getCheckOutDate())
                .nights((int) nights)
                .guestCount(orderDTO.getGuestCount())
                .price(pricePerNight)
                .totalAmount(totalAmount)
                .remark(orderDTO.getRemark())
                .build();

        // 添加额外信息，用于前端显示
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("baseAmount", baseAmount);
        additionalInfo.put("cleaningFee", cleaningFee);
        additionalInfo.put("serviceFee", serviceFee);
        additionalInfo.put("pricePerNight", pricePerNight);
        additionalInfo.put("imageUrl", homestay.getCoverImage());
        additionalInfo.put("addressDetail", homestay.getAddressDetail());
        additionalInfo.put("provinceText", homestay.getProvinceText());
        additionalInfo.put("cityText", homestay.getCityText());
        additionalInfo.put("districtText", homestay.getDistrictText());

        // 这里我们不能直接在OrderDTO添加additionalInfo字段，
        // 因为它没有定义这个字段，我们需要修改前端代码来处理这些额外信息
        // 或者扩展OrderDTO类。这里为了简单，我们返回基本信息。

        return previewOrderDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAdminOrders(
            Pageable pageable,
            String orderNumber,
            String guestName,
            String homestayTitle,
            String status,
            String paymentStatus,
            String paymentMethod,
            String hostName,
            LocalDate checkInDateStart,
            LocalDate checkInDateEnd,
            LocalDate createTimeStart,
            LocalDate createTimeEnd) {
        log.info(
                "Admin获取订单列表，筛选条件: orderNumber={}, guestName={}, homestayTitle={}, status={}, paymentStatus={}, paymentMethod={}, hostName={}, checkInDateStart={}, checkInDateEnd={}, createTimeStart={}, createTimeEnd={}",
                orderNumber, guestName, homestayTitle, status, paymentStatus, paymentMethod, hostName, checkInDateStart,
                checkInDateEnd, createTimeStart, createTimeEnd);

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 订单号
            if (orderNumber != null && !orderNumber.isEmpty()) {
                predicates.add(cb.like(root.get("orderNumber"), "%" + orderNumber + "%"));
            }

            // 订单状态
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 支付状态
            if (paymentStatus != null && !paymentStatus.isEmpty()) {
                try {
                    PaymentStatus ps = PaymentStatus.valueOf(paymentStatus.toUpperCase());
                    predicates.add(cb.equal(root.get("paymentStatus"), ps));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的支付状态筛选值: {}", paymentStatus);
                }
            }

            // 支付方式
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }

            // 入住日期范围
            if (checkInDateStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), checkInDateStart));
            }
            if (checkInDateEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkInDate"), checkInDateEnd)); // 通常筛选入住开始日期
            }

            // 创建日期范围
            if (createTimeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createTimeStart.atStartOfDay()));
            }
            if (createTimeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createTimeEnd.plusDays(1).atStartOfDay())); // 包含结束当天
            }

            // 关联查询条件
            Join<Order, User> guestJoin = null;
            Join<Order, Homestay> homestayJoin = null;
            Join<Homestay, User> hostJoin = null;

            // 住客姓名
            if (guestName != null && !guestName.isEmpty()) {
                guestJoin = root.join("guest");
                predicates.add(cb.like(guestJoin.get("username"), "%" + guestName + "%")); // 假设按用户名搜索
            }

            // 房源标题
            if (homestayTitle != null && !homestayTitle.isEmpty()) {
                if (homestayJoin == null)
                    homestayJoin = root.join("homestay");
                predicates.add(cb.like(homestayJoin.get("title"), "%" + homestayTitle + "%"));
            }

            // 房东姓名/用户名
            if (hostName != null && !hostName.isEmpty()) {
                if (homestayJoin == null)
                    homestayJoin = root.join("homestay");
                hostJoin = homestayJoin.join("owner");
                predicates.add(cb.like(hostJoin.get("username"), "%" + hostName + "%")); // 假设按用户名搜索
            }

            // 确保不返回重复记录（如果使用了多个Join）
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public OrderDTO confirmPayment(Long id) {
        // 委托给PaymentProcessingService处理
        return paymentProcessingService.confirmPayment(id);
    }

    @Override
    @Transactional
    public OrderDTO executeRefund(Long id, String reason) {
        log.info("管理员直接执行退款，订单ID: {}, 原因: {}", id, reason);
        return paymentProcessingService.executeRefund(id, reason);
    }

    @Override
    @Transactional
    public OrderDTO approveRefund(Long id, String refundNote) {
        log.info("审批退款申请，订单ID: {}, 备注: {}", id, refundNote);
        return paymentProcessingService.approveRefund(id, refundNote);
    }

    @Override
    @Transactional
    public OrderDTO rejectOrder(Long id, String reason) {
        // 委托给OrderLifecycleService处理核心生命周期逻辑
        return orderLifecycleService.rejectOrder(id, reason);
    }

    @Override
    @Transactional
    public OrderDTO rejectRefund(Long id, String rejectReason) {
        log.info("拒绝退款申请，订单ID: {}, 拒绝原因: {}", id, rejectReason);
        return paymentProcessingService.rejectRefund(id, rejectReason);
    }

    @Override
    @Transactional
    public OrderDTO requestUserRefund(Long id, String reason) {
        // 委托给PaymentProcessingService处理
        return paymentProcessingService.requestUserRefund(id, reason);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + id));

        // 检查订单状态，只允许删除已完成或已取消的订单
        if (!Arrays.asList("COMPLETED", "CANCELLED", "REJECTED").contains(order.getStatus())) {
            throw new IllegalStateException("只能删除已完成、已取消或已拒绝的订单");
        }

        orderRepository.delete(order);
    }

    @Override
    public byte[] exportOrders(Map<String, String> params) {
        // 获取符合条件的所有订单
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 按订单号筛选
            if (params.containsKey("orderNumber") && !params.get("orderNumber").isEmpty()) {
                predicates.add(cb.like(root.get("orderNumber"), "%" + params.get("orderNumber") + "%"));
            }

            // 按状态筛选
            if (params.containsKey("status") && !params.get("status").isEmpty()) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }

            // 按日期范围筛选
            if (params.containsKey("startDate") && !params.get("startDate").isEmpty()) {
                LocalDate startDate = LocalDate.parse(params.get("startDate"));
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), startDate));
            }

            if (params.containsKey("endDate") && !params.get("endDate").isEmpty()) {
                LocalDate endDate = LocalDate.parse(params.get("endDate"));
                predicates.add(cb.lessThanOrEqualTo(root.get("checkOutDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Order> orders = orderRepository.findAll(spec);

        // 创建导出数据（使用简单的CSV格式）
        try {
            StringBuilder csv = new StringBuilder();
            // 添加CSV头
            csv.append("订单编号,房源名称,入住日期,退房日期,晚数,房客姓名,房客电话,总金额,状态,创建时间\n");

            // 添加订单数据
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Order order : orders) {
                csv.append(order.getOrderNumber()).append(",");
                csv.append(order.getHomestay().getTitle()).append(",");
                csv.append(order.getCheckInDate().format(dateFormatter)).append(",");
                csv.append(order.getCheckOutDate().format(dateFormatter)).append(",");
                csv.append(order.getNights()).append(",");
                csv.append(order.getGuest().getFullName()).append(",");
                csv.append(order.getGuestPhone()).append(",");
                csv.append(order.getTotalAmount()).append(",");
                csv.append(order.getStatus()).append(",");
                csv.append(order.getCreatedAt().format(dateTimeFormatter)).append("\n");
            }

            return csv.toString().getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("导出订单失败", e);
        }
    }

    // ========== 管理员异常订单统计 ==========

    @Override
    public Map<String, Long> getExceptionOrderStats() {
        Map<String, Long> stats = new HashMap<>();

        // 待处理超时订单（PENDING超过24小时）
        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        stats.put("pendingTimeout", orderRepository.countPendingTimeoutOrders(dayAgo));

        // 支付失败订单
        stats.put("paymentFailed", orderRepository.countPaymentFailedOrders());

        // 退款失败订单
        stats.put("refundFailed", orderRepository.countRefundFailedOrders());

        // 已支付但未入住（超过入住日期）
        stats.put("notCheckedIn", orderRepository.countNotCheckedInOrders(LocalDate.now()));

        // 退款处理中
        stats.put("refundPending", orderRepository.countRefundPendingOrders());

        // 争议待处理
        stats.put("disputePending", orderRepository.countDisputePendingOrders());

        // 待确认订单（今天及之前创建的）
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        stats.put("pendingConfirm", orderRepository.countPendingConfirmOrders(startOfToday));

        // 计算异常订单总数
        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        stats.put("total", total);

        return stats;
    }

    // 工具方法：获取当前登录用户
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    // 工具方法：生成订单号
    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDate.now().format(formatter);
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "HS" + datePart + randomPart;
    }

    // 工具方法：检查用户是否为订单的房东
    private boolean isOrderOwner(Order order, User user) {
        return order.getHomestay() != null && order.getHomestay().getOwner() != null
                && order.getHomestay().getOwner().getId().equals(user.getId());
    }

    // 工具方法：检查用户是否为订单的客户
    private boolean isOrderGuest(Order order, User user) {
        return order.getGuest() != null && order.getGuest().getId().equals(user.getId());
    }

    private boolean hasAdminAuthority(User user) {
        if (user == null || user.getRole() == null) { // 假设 getRole() 返回 String
            return false;
        }
        // 与 isOrderAccessible 方法中的 isAdmin 逻辑保持一致
        return user.getRole().contains("ADMIN");
    }

    /**
     * 检查用户是否有权访问和操作此订单
     * 
     * @param order 订单对象
     * @param user  当前用户
     * @return 如果用户有权访问此订单则返回true
     */
    private boolean isOrderAccessible(Order order, User user) {
        log.debug("检查订单访问权限 - 订单ID: {}, 用户: {}, 角色: {}",
                order.getId(), user.getUsername(), user.getRole());
        log.debug("订单所属房东ID: {}, 当前用户ID: {}",
                order.getHomestay().getOwner().getId(), user.getId());
        log.debug("订单客人ID: {}", order.getGuest().getId());

        // 检查用户角色 - 考虑可能有前缀或不一致的情况
        boolean isAdmin = user.getRole().contains("ADMIN");

        // 管理员可以访问所有订单
        if (isAdmin) {
            log.debug("用户是管理员，允许访问");
            return true;
        }

        // 房东可以访问自己房源的订单
        boolean isHost = user.getRole().contains("HOST");
        if (isHost) {
            boolean isOwner = isOrderOwner(order, user);
            log.debug("用户是房东，是否为订单所属房东: {}", isOwner);
            if (isOwner) {
                return true;
            }
        }

        // 客户可以访问自己的订单
        boolean isUser = user.getRole().contains("USER");
        if (isUser) {
            boolean isGuest = isOrderGuest(order, user);
            log.debug("用户是普通用户，是否为订单客人: {}", isGuest);
            if (isGuest) {
                return true;
            }
        }

        log.debug("权限检查失败，拒绝访问");
        return false;
    }

    // 工具方法：将 Order 实体转换为 OrderDTO
    private OrderDTO convertToDTO(Order order) {
        if (order == null) {
            return null;
        }

        // 检查订单是否已被评价
        boolean isReviewed = reviewRepository.existsByOrder(order);
        ReviewDTO reviewDTO = null;

        // 如果订单已完成且已评价，则获取评价详情
        if (OrderStatus.COMPLETED.name().equals(order.getStatus()) && isReviewed) {
            Optional<Review> reviewOpt = reviewRepository.findByOrder(order);
            if (reviewOpt.isPresent()) {
                reviewDTO = convertReviewToDTO(reviewOpt.get()); // 调用辅助方法转换
            }
        }

        // 获取房东信息，如果存在的话（处理homestay已被删除的边缘情况）
        User host = null;
        String hostName = null;
        Long hostId = null;
        try {
            if (order.getHomestay() != null) {
                host = order.getHomestay().getOwner();
                if (host != null) {
                    hostName = host.getNickname() != null ? host.getNickname() : host.getUsername();
                    hostId = host.getId();
                }
            }
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // homestay已被删除，忽略异常，保持hostName和hostId为null
            log.warn("订单 {} 关联的homestay已被删除", order.getId());
        }

        // 获取房客信息，如果存在的话（处理用户已被删除的边缘情况）
        String guestName = null;
        Long guestId = null;
        try {
            User guest = order.getGuest();
            if (guest != null) {
                guestName = guest.getNickname() != null ? guest.getNickname() : guest.getUsername();
                guestId = guest.getId();
            }
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // guest已被删除，忽略异常
            log.warn("订单 {} 关联的guest用户已被删除", order.getId());
        }

        // 获取退款相关的用户名
        String refundInitiatedByName = null;
        String refundProcessedByName = null;

        if (order.getRefundInitiatedBy() != null) {
            Optional<User> initiatorOpt = userRepository.findById(order.getRefundInitiatedBy());
            if (initiatorOpt.isPresent()) {
                User initiator = initiatorOpt.get();
                refundInitiatedByName = initiator.getNickname() != null ? initiator.getNickname()
                        : initiator.getUsername();
            }
        }

        if (order.getRefundProcessedBy() != null) {
            Optional<User> processorOpt = userRepository.findById(order.getRefundProcessedBy());
            if (processorOpt.isPresent()) {
                User processor = processorOpt.get();
                refundProcessedByName = processor.getNickname() != null ? processor.getNickname()
                        : processor.getUsername();
            }
        }

        // 从系统配置动态读取费用配置
        BigDecimal baseAmount = order.getPrice() != null ? order.getPrice().multiply(BigDecimal.valueOf(order.getNights())) : BigDecimal.ZERO;
        // 清洁费：固定金额 = 单晚价格 × 配置比例
        BigDecimal cleaningFeeAmount = getPricingConfig("pricing.cleaning_fee", "0.1");
        BigDecimal serviceFeeRate = getPricingConfig("pricing.service_fee", "0.15");
        BigDecimal cleaningFee = order.getPrice() != null ? order.getPrice().multiply(cleaningFeeAmount) : BigDecimal.ZERO;
        BigDecimal serviceFee = baseAmount.multiply(serviceFeeRate);

        // 获取homestay信息（处理homestay已被删除的边缘情况）
        Long homestayId = null;
        String homestayTitle = null;
        try {
            if (order.getHomestay() != null) {
                homestayId = order.getHomestay().getId();
                homestayTitle = order.getHomestay().getTitle();
            }
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // homestay已被删除，忽略异常
            log.warn("订单 {} 关联的homestay已被删除", order.getId());
        }

        // 构建 OrderDTO
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .homestayId(homestayId)
                .homestayTitle(homestayTitle)
                .guestId(guestId)
                .guestName(guestName)
                .guestPhone(order.getGuestPhone())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .guestCount(order.getGuestCount())
                .price(order.getPrice())
                .cleaningFee(cleaningFee)
                .serviceFee(serviceFee)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentMethod(order.getPaymentMethod())
                .remark(order.getRemark())
                .hostId(hostId)
                .hostName(hostName)
                .createTime(order.getCreatedAt())
                .updateTime(order.getUpdatedAt())
                .isReviewed(isReviewed)
                .review(reviewDTO)
                // 退款相关字段
                .refundType(order.getRefundType() != null ? order.getRefundType().name() : null)
                .refundReason(order.getRefundReason())
                .refundAmount(order.getRefundAmount())
                .refundInitiatedBy(order.getRefundInitiatedBy())
                .refundInitiatedByName(refundInitiatedByName)
                .refundInitiatedAt(order.getRefundInitiatedAt())
                .refundProcessedBy(order.getRefundProcessedBy())
                .refundProcessedByName(refundProcessedByName)
                .refundProcessedAt(order.getRefundProcessedAt())
                .refundTransactionId(order.getRefundTransactionId())
                .refundRejectionReason(order.getRefundRejectionReason())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRefundPreview(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));

        // 权限检查：只有订单相关方才能查看退款预览
        User currentUser = getCurrentUser();
        if (!isOrderAccessible(order, currentUser)) {
            throw new AccessDeniedException("您无权查看此订单的退款信息");
        }

        // 检查是否可以发起退款
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return Map.of(
                "eligible", false,
                "message", "订单未处于可退款状态（当前支付状态: " + (order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "未知") + "）"
            );
        }

        Map<String, Object> preview = new HashMap<>(buildRefundPreviewInfo(order));
        preview.put("eligible", true);
        preview.put("orderId", orderId);
        preview.put("orderNumber", order.getOrderNumber());
        return preview;
    }

    // --- 添加私有辅助方法：转换 Review 到 ReviewDTO ---
    private ReviewDTO convertReviewToDTO(Review review) {
        if (review == null) {
            return null;
        }
        // (此逻辑复制自 ReviewServiceImpl 的 convertToDTO，注意字段是否匹配)
        return ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getUsername() : null)
                .userAvatar(review.getUser() != null ? review.getUser().getAvatar() : null)
                .homestayId(review.getHomestay() != null ? review.getHomestay().getId() : null)
                .homestayTitle(review.getHomestay() != null ? review.getHomestay().getTitle() : null)
                .orderId(review.getOrder() != null ? review.getOrder().getId() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .cleanlinessRating(review.getCleanlinessRating())
                .accuracyRating(review.getAccuracyRating())
                .communicationRating(review.getCommunicationRating())
                .locationRating(review.getLocationRating())
                .checkInRating(review.getCheckInRating())
                .valueRating(review.getValueRating())
                .response(review.getResponse())
                .responseTime(review.getResponseTime())
                .createTime(review.getCreateTime())
                .isPublic(review.getIsPublic())
                // isOwnerResponse 在 OrderServiceImpl 中难以判断，可留空或移除
                .build();
    }

    // ========== 价格计算 ==========

    @Override
    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        // 1. 参数验证
        if (request.getHomestayId() == null) {
            throw new IllegalArgumentException("房源ID不能为空");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("入住日期和退房日期不能为空");
        }
        if (request.getCheckInDate().isAfter(request.getCheckOutDate()) ||
            request.getCheckInDate().isEqual(request.getCheckOutDate())) {
            throw new IllegalArgumentException("退房日期必须晚于入住日期");
        }

        // 2. 获取房源信息
        Homestay homestay = homestayRepository.findById(request.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));

        // 3. 验证人数
        if (request.getGuestCount() != null && request.getGuestCount() > homestay.getMaxGuests()) {
            throw new IllegalArgumentException("入住人数不能超过房源最大容纳人数：" + homestay.getMaxGuests());
        }

        // 4. 计算住宿晚数
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights < homestay.getMinNights()) {
            throw new IllegalArgumentException("住宿天数不能少于" + homestay.getMinNights() + "晚");
        }

        // 5. 获取系统配置的费用配置
        // 清洁费：固定金额 = 单晚价格 × 配置比例（一次性，不论住多少晚）
        BigDecimal cleaningFeeAmount = getPricingConfig("pricing.cleaning_fee", "0.1");
        // 服务费：按订单总价的百分比收取
        BigDecimal serviceFeeRate = getPricingConfig("pricing.service_fee", "0.15");
        BigDecimal weekendRate = getPricingConfig("pricing.weekend_rate", "1.2");
        BigDecimal holidayRate = getPricingConfig("pricing.holiday_rate", "1.5");

        // 6. 获取房源折扣
        BigDecimal discountRate = getHomestayDiscount(request.getHomestayId());

        // 7. 计算每日价格
        BigDecimal basePricePerNight = homestay.getPrice();
        List<PriceCalculationResponse.DailyPrice> dailyPrices = new ArrayList<>();
        BigDecimal totalBasePrice = BigDecimal.ZERO;

        LocalDate currentDate = request.getCheckInDate();
        for (int i = 0; i < nights; i++) {
            LocalDate date = currentDate.plusDays(i);
            BigDecimal dailyPrice = calculateDailyPrice(date, basePricePerNight, weekendRate, holidayRate);

            String holidayName = getHolidayName(date);

            dailyPrices.add(PriceCalculationResponse.DailyPrice.builder()
                    .date(date)
                    .price(dailyPrice)
                    .isWeekend(date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                    .isHoliday(holidayName != null)
                    .holidayName(holidayName)
                    .build());

            totalBasePrice = totalBasePrice.add(dailyPrice);
        }

        // 8. 计算折扣
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalBasePrice = totalBasePrice;
        if (discountRate != null && discountRate.compareTo(BigDecimal.ONE) < 0) {
            discountAmount = totalBasePrice.multiply(BigDecimal.ONE.subtract(discountRate));
            finalBasePrice = totalBasePrice.subtract(discountAmount);
        }

        // 9. 计算清洁费和服务费
        // 清洁费：固定金额 = 单晚价格 × 配置比例（不论住多少晚都一样）
        BigDecimal cleaningFee = homestay.getPrice().multiply(cleaningFeeAmount).setScale(2, RoundingMode.HALF_UP);
        // 服务费：按折后订单总价的百分比收取
        BigDecimal serviceFee = finalBasePrice.multiply(serviceFeeRate).setScale(2, RoundingMode.HALF_UP);

        // 10. 计算总价
        BigDecimal totalPrice = finalBasePrice.add(cleaningFee).add(serviceFee);

        // 11. 构建响应
        return PriceCalculationResponse.builder()
                .homestayId(homestay.getId())
                .homestayTitle(homestay.getTitle())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .guestCount(request.getGuestCount())
                .nights((int) nights)
                .dailyPrices(dailyPrices)
                .basePrice(totalBasePrice)
                .discountAmount(discountAmount)
                .finalBasePrice(finalBasePrice)
                .cleaningFee(cleaningFee)
                .serviceFee(serviceFee)
                .totalPrice(totalPrice)
                .priceDetails(PriceCalculationResponse.PriceDetails.builder()
                        .cleaningFeeAmount(cleaningFeeAmount)
                        .serviceFeeRate(serviceFeeRate)
                        .weekendRate(weekendRate)
                        .holidayRate(holidayRate)
                        .discountRate(discountRate)
                        .build())
                .build();
    }

    /**
     * 获取定价配置，默认值
     */
    private BigDecimal getPricingConfig(String key, String defaultValue) {
        String value = systemConfigServiceProvider.getObject().getConfigValue(key, defaultValue);
        log.info("读取配置: {} = {} (默认: {})", key, value, defaultValue);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("定价配置 {} 格式错误，使用默认值 {}，错误: {}", key, defaultValue, e.getMessage());
            return new BigDecimal(defaultValue);
        }
    }

    /**
     * 获取房源折扣率
     */
    private BigDecimal getHomestayDiscount(Long homestayId) {
        String discountKey = "homestay.discount." + homestayId;
        String discountValue = systemConfigServiceProvider.getObject().getConfigValue(discountKey);
        if (discountValue != null) {
            try {
                return new BigDecimal(discountValue);
            } catch (NumberFormatException e) {
                log.warn("房源折扣配置 {} 格式错误: {}", discountKey, e.getMessage());
            }
        }
        return null; // 无折扣
    }

    /**
     * 计算单日价格（考虑周末和节假日）
     */
    private BigDecimal calculateDailyPrice(LocalDate date, BigDecimal basePrice,
            BigDecimal weekendRate, BigDecimal holidayRate) {
        BigDecimal rate = BigDecimal.ONE;

        // 检查节假日
        String holidayName = getHolidayName(date);
        if (holidayName != null) {
            rate = holidayRate;
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            rate = weekendRate;
        }

        return basePrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取日期对应的节假日名称（简单实现，可扩展）
     */
    private String getHolidayName(LocalDate date) {
        // 中国2026年主要节假日（简单版本，实际应从数据库或API获取）
        Map<LocalDate, String> holidays = new HashMap<>();

        // 元旦
        holidays.put(LocalDate.of(2026, 1, 1), "元旦");

        // 春节（2026年2月17日-2月23日，假设7天）
        for (int i = 17; i <= 23; i++) {
            holidays.put(LocalDate.of(2026, 2, i), "春节");
        }

        // 清明节（2026年4月4日-4月6日）
        for (int i = 4; i <= 6; i++) {
            holidays.put(LocalDate.of(2026, 4, i), "清明节");
        }

        // 劳动节（2026年5月1日-5月3日）
        for (int i = 1; i <= 3; i++) {
            holidays.put(LocalDate.of(2026, 5, i), "劳动节");
        }

        // 端午节（2026年5月31日-6月2日）
        for (int i = 31; i <= 31; i++) {
            holidays.put(LocalDate.of(2026, 5, i), "端午节");
        }
        for (int i = 1; i <= 2; i++) {
            holidays.put(LocalDate.of(2026, 6, i), "端午节");
        }

        // 中秋节（2026年10月1日-10月3日，与国庆合并）
        for (int i = 1; i <= 3; i++) {
            holidays.put(LocalDate.of(2026, 10, i), "中秋节");
        }

        // 国庆节（2026年10月1日-10月7日）
        for (int i = 1; i <= 7; i++) {
            holidays.put(LocalDate.of(2026, 10, i), "国庆节");
        }

        return holidays.get(date);
    }
}