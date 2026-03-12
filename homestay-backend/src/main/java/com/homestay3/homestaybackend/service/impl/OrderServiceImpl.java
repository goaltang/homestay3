package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
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
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.service.OrderService;
import com.homestay3.homestaybackend.service.EarningService;
import com.homestay3.homestaybackend.service.BookingConflictService;
import com.homestay3.homestaybackend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final NotificationService notificationService;
    private final OrderNotificationService orderNotificationService;
    private final EarningService earningService;
    private final ReviewRepository reviewRepository;
    private final BookingConflictService bookingConflictService;
    private final PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        log.info("开始创建订单: 用户={}, 房源={}, 入住日期={}, 退房日期={}",
                getCurrentUser().getUsername(), orderDTO.getHomestayId(),
                orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());

        User currentUser = getCurrentUser();

        // 1. 获取房源信息（不再使用数据库级悲观锁，改由 BookingConflictService 的 Redis 分布式锁保护）
        Homestay homestay = homestayRepository.findById(orderDTO.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));

        if (!homestay.getStatus().equals(HomestayStatus.ACTIVE)) {
            throw new IllegalArgumentException("房源当前不可预订");
        }

        if (currentUser.getId().equals(homestay.getOwner().getId())) {
            throw new IllegalArgumentException("不能预订自己的房源");
        }

        // 2. 验证日期
        if (orderDTO.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }
        if (orderDTO.getCheckOutDate().isBefore(orderDTO.getCheckInDate())) {
            throw new IllegalArgumentException("退房日期不能早于入住日期");
        }

        // 4. 计算住宿天数
        long nights = ChronoUnit.DAYS.between(orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (nights < homestay.getMinNights()) {
            throw new IllegalArgumentException("住宿天数不能少于" + homestay.getMinNights() + "晚");
        }

        if (orderDTO.getGuestCount() == null || orderDTO.getGuestCount() <= 0) {
            throw new IllegalArgumentException("入住人数必须大于0");
        }
        if (orderDTO.getGuestCount() > homestay.getMaxGuests()) {
            throw new IllegalArgumentException("入住人数不能超过房源最大入住人数" + homestay.getMaxGuests() + "人");
        }

        // 5. 计算总价
        BigDecimal baseAmount = homestay.getPrice().multiply(BigDecimal.valueOf(nights));
        BigDecimal cleaningFee = homestay.getPrice().multiply(BigDecimal.valueOf(0.1));
        BigDecimal serviceFee = baseAmount.multiply(BigDecimal.valueOf(0.15));
        BigDecimal totalAmount = baseAmount.add(cleaningFee).add(serviceFee);
        log.info("计算订单总价: {} (基础房费: {}, 清洁费: {}, 服务费: {})", totalAmount, baseAmount, cleaningFee, serviceFee);

        // 6. 根据房源自动确认设置决定初始订单状态
        String initialStatus;
        if (homestay.getAutoConfirm() != null && homestay.getAutoConfirm()) {
            initialStatus = OrderStatus.CONFIRMED.name();
            log.info("房源 {} 开启自动确认，订单将直接进入已确认状态", homestay.getId());
        } else {
            initialStatus = OrderStatus.PENDING.name();
            log.info("房源 {} 采用房东确认制，订单进入待确认状态", homestay.getId());
        }

        // 7. 创建订单对象
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .homestay(homestay)
                .guest(currentUser)
                .guestPhone(orderDTO.getGuestPhone())
                .checkInDate(orderDTO.getCheckInDate())
                .checkOutDate(orderDTO.getCheckOutDate())
                .nights((int) nights)
                .guestCount(orderDTO.getGuestCount())
                .price(homestay.getPrice())
                .totalAmount(totalAmount)
                .status(initialStatus)
                .remark(orderDTO.getRemark())
                .build();

        try {
            // 8. 使用安全的创建方法（带锁保护）
            Order savedOrder = bookingConflictService.safeCreateOrder(order);
            log.info("订单创建成功: id={}, orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());

            // 9. 根据订单状态发送不同的通知
            try {
                User host = homestay.getOwner();
                if (OrderStatus.CONFIRMED.name().equals(savedOrder.getStatus())) {
                    // 自动确认房源：通知房东有新订单，通知客人可以支付
                    orderNotificationService.sendOrderAutoConfirmedNotification(
                            host.getId(), currentUser.getId(), savedOrder.getId(),
                            host.getUsername(), currentUser.getUsername(), savedOrder.getOrderNumber());
                } else {
                    // 房东确认制：通知房东有新预订请求
                    orderNotificationService.sendOrderBookingRequestNotification(
                            host.getId(), currentUser.getId(), savedOrder.getId(),
                            currentUser.getUsername(), savedOrder.getOrderNumber());
                }
            } catch (Exception e) {
                log.error("发送通知失败: {}", e.getMessage());
            }
            } catch (Exception e) {
                log.error("发送通知失败: {}", e.getMessage());
            }

            // 10. 设置支付状态
            savedOrder.setPaymentStatus(PaymentStatus.UNPAID);
            savedOrder = orderRepository.save(savedOrder);

            return convertToDTO(savedOrder);

        } catch (DataIntegrityViolationException e) {
            log.error("订单创建失败，可能存在日期冲突: {}", e.getMessage());
            throw new IllegalArgumentException("所选日期已被预订，请选择其他日期");
        } catch (Exception e) {
            log.error("订单创建过程中发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("订单创建失败，请稍后重试");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查权限
        User currentUser = getCurrentUser();
        if (!isOrderAccessible(order, currentUser)) {
            throw new AccessDeniedException("您无权访问此订单");
        }

        return convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查权限
        User currentUser = getCurrentUser();
        if (!isOrderAccessible(order, currentUser)) {
            throw new AccessDeniedException("您无权访问此订单");
        }

        return convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getMyOrders(Map<String, String> params, Pageable pageable) {
        User currentUser = getCurrentUser();

        // 创建查询条件
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 当前用户作为客人
            predicates.add(cb.equal(root.get("guest"), currentUser));

            // 按状态筛选
            if (params.containsKey("status")) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOwnerOrders(String ownerUsername, Map<String, String> params, Pageable pageable) {
        // 获取房东用户
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 创建查询条件
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关联房源表，筛选出指定房东的订单
            Join<Order, Homestay> homestayJoin = root.join("homestay");
            predicates.add(cb.equal(homestayJoin.get("owner"), owner));

            // 按状态筛选
            if (params.containsKey("status")) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }

            // 按房源ID筛选
            if (params.containsKey("homestayId")) {
                predicates.add(cb.equal(homestayJoin.get("id"),
                        Long.parseLong(params.get("homestayId"))));
            }

            // 按日期范围筛选
            if (params.containsKey("startDate") && params.containsKey("endDate")) {
                LocalDate startDate = LocalDate.parse(params.get("startDate"));
                LocalDate endDate = LocalDate.parse(params.get("endDate"));

                predicates.add(cb.or(
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("checkInDate"), startDate),
                                cb.lessThanOrEqualTo(root.get("checkInDate"), endDate)),
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("checkOutDate"), startDate),
                                cb.lessThanOrEqualTo(root.get("checkOutDate"), endDate))));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPendingOrderCount(String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        return orderRepository.countPendingOrdersByOwner(owner);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查当前用户是否有权限更新此订单状态
        User currentUser = getCurrentUser();
        log.debug("更新订单状态请求 - 订单ID: {}, 目标状态: {}, 用户: {}",
                id, status, currentUser.getUsername());

        if (!isOrderAccessible(order, currentUser)) {
            log.warn("用户 {} 无权更新订单 {} 的状态", currentUser.getUsername(), id);
            throw new AccessDeniedException("您无权更新此订单状态");
        }

        // 检查目标状态字符串是否有效
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.error("无效的目标状态字符串: {}", status);
            throw new IllegalArgumentException("无效的订单状态: " + status);
        }

        // 检查状态转换是否有效
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());
        log.debug("订单 {} 当前状态: {}, 目标状态: {}", id, currentStatus, targetStatus);
        if (!isValidStatusTransition(currentStatus, targetStatus)) {
            log.warn("无效的状态转换尝试：从 {} 到 {} (订单 ID: {})", currentStatus, targetStatus, id);
            throw new IllegalArgumentException("无效的订单状态转换: 从 " + currentStatus + " 到 " + targetStatus);
        }

        log.info("准备更新订单 {} 状态从 {} 到 {}", order.getOrderNumber(), currentStatus, targetStatus);

        // --- 修改：默认先设置目标状态 ---
        order.setStatus(targetStatus.name());
        log.debug("订单 {} 状态临时设置为 {}", id, targetStatus.name());

        // --- 添加：如果目标状态是PAID，则同步更新支付状态 ---
        if (targetStatus == OrderStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            log.debug("订单 {} 支付状态同步更新为 PAID", id);
        }

            // 根据目标状态执行特定逻辑 (现在主要是添加通知等额外操作)
            if (targetStatus == OrderStatus.CONFIRMED) {
                if (!isOrderOwner(order, currentUser)) {
                    log.warn("非房东 {} 尝试确认订单 {}", currentUser.getUsername(), id);
                    throw new AccessDeniedException("只有房东才能确认订单");
                }
                log.info("房东 {} 正在确认订单 {}", currentUser.getUsername(), id);
                // 状态已在前面设置，这里只处理通知
                // --- 添加：发送订单确认通知给房客 ---
                try {
                    User guest = order.getGuest();
                    orderNotificationService.sendOrderConfirmedNotification(
                            guest.getId(), currentUser.getId(), order.getId(),
                            guest.getUsername(), order.getHomestay().getTitle(),
                            currentUser.getUsername(), order.getOrderNumber());
                } catch (Exception e) {
                    log.error("为房客 {} 发送订单 {} 确认通知失败: {}", order.getGuest().getUsername(), order.getOrderNumber(),
                            e.getMessage(), e);
                }
                // --- 通知发送结束 ---

        } else if (targetStatus == OrderStatus.CANCELLED ||
                targetStatus == OrderStatus.CANCELLED_BY_HOST ||
                targetStatus == OrderStatus.CANCELLED_BY_USER ||
                targetStatus == OrderStatus.CANCELLED_SYSTEM) {
            // 取消订单逻辑现在由 cancelOrder 方法处理，这里可以简化或移除
            // 如果保留，需要区分是谁取消的，并发送通知
            log.warn("通过 updateOrderStatus 直接更新为 CANCELLED 状态，建议使用 cancelOrder 方法以包含原因和发送通知。");
            // 状态已在前面设置

        } else if (targetStatus == OrderStatus.COMPLETED) {
            // 检查当前用户是否有权标记完成 (房东、房客或管理员)
            boolean isAdmin = currentUser.getRole().contains("ADMIN"); // 假设角色字符串包含 ADMIN
            if (!isOrderOwner(order, currentUser) && !isOrderGuest(order, currentUser) && !isAdmin) {
                log.warn("用户 {} (非房东/房客/管理员) 尝试标记订单 {} 为完成", currentUser.getUsername(), id);
                throw new AccessDeniedException("无权标记此订单为完成状态");
            }
            // 确保订单已支付或在某些允许的状态
            if (currentStatus != OrderStatus.PAID && currentStatus != OrderStatus.CHECKED_IN) { // 或其他允许完成的状态
                log.warn("订单 {} 状态为 {}，无法直接标记为完成", id, currentStatus);
                throw new IllegalArgumentException("订单状态为 " + currentStatus + "，无法直接标记为完成");
            }
            log.info("用户 {} 正在标记订单 {} 为完成", currentUser.getUsername(), id);
            // 状态已在前面设置

            // --- 添加：调用 EarningService 生成待结算收益 ---
            try {
                log.info("订单 {} 完成，尝试生成待结算收益记录...", order.getOrderNumber());
                // 调用 EarningService 生成待结算收益记录
                earningService.generatePendingEarningForOrder(order.getId());
                log.info("订单 {} 的待结算收益记录已生成。", order.getOrderNumber());
            } catch (Exception e) {
                // 记录错误，但可能不应阻止订单完成
                log.error("为订单 {} 生成待结算收益时出错: {}", order.getOrderNumber(), e.getMessage(), e);
                // 可以考虑添加一些错误处理或标记，以便后续跟踪
            }
            // --- 收益生成结束 ---

            // --- 添加：发送订单完成通知 ---
            try {
                orderNotificationService.sendOrderCompletedNotification(
                        order.getGuest().getId(),
                        order.getHomestay().getOwner().getId(),
                        order.getId(),
                        order.getGuest().getUsername(),
                        order.getHomestay().getOwner().getUsername(),
                        order.getHomestay().getTitle(),
                        order.getOrderNumber(),
                        currentUser.getId());
            } catch (Exception notifyEx) {
                log.error("发送订单 {} 完成通知失败: {}", order.getOrderNumber(), notifyEx.getMessage(), notifyEx);
            }
            // --- 通知发送结束 ---

        } else if (targetStatus == OrderStatus.REJECTED) {
            // 拒绝订单逻辑现在由 rejectOrder 方法处理
            log.warn("通过 updateOrderStatus 直接更新为 REJECTED 状态，建议使用 rejectOrder 方法以包含原因和发送通知");
            // 状态已在前面设置

        } else if (targetStatus == OrderStatus.PAYMENT_PENDING) {
            log.info("订单 {} 状态将更新为 PAYMENT_PENDING", id);
            // 状态已在前面设置，无需额外操作

        } // 可以根据需要添加其他状态的特定逻辑，例如 CHECKED_IN
        else if (targetStatus == OrderStatus.CHECKED_IN) {
            if (!isOrderOwner(order, currentUser) && !isOrderGuest(order, currentUser)) {
                log.warn("非房东或房客 {} 尝试标记订单 {} 为入住", currentUser.getUsername(), id);
                throw new AccessDeniedException("无权标记此订单为入住状态");
            }
            if (currentStatus != OrderStatus.PAID && currentStatus != OrderStatus.READY_FOR_CHECKIN) {
                log.warn("订单 {} 状态为 {}，无法直接标记为入住", id, currentStatus);
                throw new IllegalArgumentException("订单状态为 " + currentStatus + "，无法直接标记为入住");
            }
            log.info("用户 {} 正在标记订单 {} 为入住", currentUser.getUsername(), id);
            // 状态已在前面设置
            // 可以考虑发送入住通知等
        }

        // 保存更新后的订单
        Order updatedOrder = orderRepository.save(order);
        log.info("订单 {} 状态已更新并保存为: {}", id, updatedOrder.getStatus());

        return convertToDTO(updatedOrder);
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
        long hoursBetween = java.time.Duration.between(now, checkInTime).toHours();

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
        // 调用带类型和原因的取消方法，使用默认类型：CANCELLED
        return cancelOrderWithReason(id, OrderStatus.CANCELLED.name(), "用户取消订单");
    }

    @Override
    @Transactional
    public OrderDTO cancelOrderWithReason(Long id, String cancelType, String reason) {
        log.info("取消订单请求 - 订单ID: {}, 取消类型: {}, 原因: {}", id, cancelType, reason);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查当前用户是否有权限取消此订单
        User currentUser = getCurrentUser();

        // 根据取消类型和当前用户角色进行权限检查
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(cancelType);
        } catch (IllegalArgumentException e) {
            // 默认取消状态
            targetStatus = OrderStatus.CANCELLED;
        }

        // 如果是用户取消，检查当前用户是否是订单的客户
        if (targetStatus == OrderStatus.CANCELLED_BY_USER) {
            if (!isOrderGuest(order, currentUser)) {
                log.warn("用户 {} 尝试取消不属于自己的订单 {}", currentUser.getUsername(), id);
                throw new AccessDeniedException("您只能取消自己的订单");
            }
        }
        // 如果是房东取消，检查当前用户是否是房源的拥有者
        else if (targetStatus == OrderStatus.CANCELLED_BY_HOST) {
            if (!isOrderOwner(order, currentUser)) {
                log.warn("用户 {} 尝试作为房东取消订单 {}，但不是该房源的拥有者", currentUser.getUsername(), id);
                throw new AccessDeniedException("您只能取消自己房源的订单");
            }
        }
        // 如果是系统/管理员取消，检查当前用户是否有管理员权限
        else if (targetStatus == OrderStatus.CANCELLED_SYSTEM || targetStatus == OrderStatus.CANCELLED) {
            if (!hasAdminAuthority(currentUser)) {
                log.warn("非管理员用户 {} 尝试进行系统取消订单 {}", currentUser.getUsername(), id);
                throw new AccessDeniedException("只有管理员可以执行系统取消操作");
            }
        }

        // 处理取消逻辑
        return processCancelOrder(order, cancelType, reason);
    }

    /**
     * 系统级取消订单方法，用于定时任务等不需要用户认证的场景
     * 此方法不检查用户权限，请谨慎使用
     */
    @Transactional
    public OrderDTO systemCancelOrder(Long id, String cancelType, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        return processCancelOrder(order, cancelType, reason);
    }

    /**
     * 处理取消订单的核心逻辑，被cancelOrderWithReason和systemCancelOrder共同使用
     */
    private OrderDTO processCancelOrder(Order order, String cancelType, String reason) {
        // 检查订单状态，只有待确认、已确认、已支付的订单可以取消
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());
        OrderStatus targetStatus;

        try {
            targetStatus = OrderStatus.valueOf(cancelType);
        } catch (IllegalArgumentException e) {
            // 如果取消类型无效，使用默认取消状态
            targetStatus = OrderStatus.CANCELLED;
        }

        // 检查取消状态是否是一种取消状态
        if (targetStatus != OrderStatus.CANCELLED &&
                targetStatus != OrderStatus.CANCELLED_BY_USER &&
                targetStatus != OrderStatus.CANCELLED_BY_HOST &&
                targetStatus != OrderStatus.CANCELLED_SYSTEM) {
            throw new IllegalArgumentException("无效的取消状态");
        }

        // 特殊处理：如果是已支付订单被取消，应该进入退款流程
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("已支付订单被取消，将进入退款流程，订单号: {}", order.getOrderNumber());

            // 已支付订单取消时，状态应该是REFUND_PENDING，而不是各种取消状态
            targetStatus = OrderStatus.REFUND_PENDING;

            // 验证状态流转是否合法（PAID -> REFUND_PENDING）
            if (!isValidStatusTransition(currentStatus, targetStatus)) {
                throw new IllegalArgumentException(
                        String.format("不允许从当前状态 [%s] 进入退款流程", currentStatus.getDescription()));
            }

            // 更新订单状态和支付状态
            order.setStatus(targetStatus.name());
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);

            // 设置退款相关信息
            User currentUser = null;
            Long actorId = null;
            String actorUsername = null;
            try {
                currentUser = getCurrentUser();
                actorId = currentUser.getId();
                actorUsername = currentUser.getUsername();
            } catch (Exception e) {
                log.warn("无法获取当前用户，可能是系统调用: {}", e.getMessage());
            }

            // 确定退款类型和发起者
            RefundType refundType;
            if (targetStatus == OrderStatus.CANCELLED_BY_USER ||
                    (currentUser != null && isOrderGuest(order, currentUser))) {
                refundType = RefundType.USER_REQUESTED;
            } else if (targetStatus == OrderStatus.CANCELLED_BY_HOST ||
                    (currentUser != null && isOrderOwner(order, currentUser))) {
                refundType = RefundType.HOST_CANCELLED;
            } else {
                refundType = RefundType.ADMIN_INITIATED;
            }

            order.setRefundType(refundType);
            order.setRefundReason(reason != null && !reason.isEmpty() ? reason : "订单取消导致退款");

            // 使用新逻辑计算退款金额
            BigDecimal calculatedRefund = calculateRefundAmount(order);
            order.setRefundAmount(calculatedRefund);

            order.setRefundInitiatedBy(actorId);
            order.setRefundInitiatedAt(LocalDateTime.now());

            // 添加退款备注
            String refundNote = String.format("已发起退款申请 - 类型: %s, 申请人: %s, 计算退款金额: %s",
                    refundType.getDescription(),
                    actorUsername != null ? actorUsername : "System",
                    calculatedRefund);
            if (order.getRemark() != null && !order.getRemark().isEmpty()) {
                order.setRemark(order.getRemark() + "\n" + refundNote);
            } else {
                order.setRemark(refundNote);
            }

            log.info("已支付订单 {} 状态已更新为退款中，退款类型: {}", order.getOrderNumber(), refundType.getDescription());
        } else {
            // 未支付订单的正常取消流程
            // 验证状态流转是否合法
            if (!isValidStatusTransition(currentStatus, targetStatus)) {
                throw new IllegalArgumentException(
                        String.format("不允许从当前状态 [%s] 取消订单", currentStatus.getDescription()));
            }

            // 更新订单状态和备注
            order.setStatus(targetStatus.name());
            if (reason != null && !reason.isEmpty()) {
                if (order.getRemark() != null && !order.getRemark().isEmpty()) {
                    order.setRemark(order.getRemark() + "\n取消原因: " + reason);
                } else {
                    order.setRemark("取消原因: " + reason);
                }
            }
        }

        Order cancelledOrder = orderRepository.save(order);

        // --- 发送订单取消/退款通知 ---
        try {
            User guest = cancelledOrder.getGuest();
            User actor = null;
            // 尝试获取当前用户作为操作者，如果失败（例如在系统调用中），actorId 将为 null
            try {
                actor = getCurrentUser();
            } catch (Exception e) {
                log.warn("无法在发送订单取消通知时获取当前用户: {}", e.getMessage());
            }

            if (guest != null) {
                String notificationContent;
                if (cancelledOrder.getStatus().equals(OrderStatus.REFUND_PENDING.name())) {
                    // 退款通知
                    notificationContent = String.format(
                            "您的订单 %s (房源: %s) 退款申请已提交，我们将在1-3个工作日内处理。原因: %s",
                            cancelledOrder.getOrderNumber(),
                            cancelledOrder.getHomestay() != null ? cancelledOrder.getHomestay().getTitle() : "未知房源",
                            reason != null && !reason.isEmpty() ? reason : "未提供原因");
                } else {
                    // 取消通知
                    notificationContent = String.format(
                            "您的订单 %s (房源: %s) 已被取消。状态从 %s 变为 %s。原因: %s",
                            cancelledOrder.getOrderNumber(),
                            cancelledOrder.getHomestay() != null ? cancelledOrder.getHomestay().getTitle() : "未知房源",
                            currentStatus.getDescription(),
                            OrderStatus.valueOf(cancelledOrder.getStatus()).getDescription(),
                            reason != null && !reason.isEmpty() ? reason : "未提供原因");
                }

                 // 发送订单取消/退款通知
                 if (cancelledOrder.getStatus().equals(OrderStatus.REFUND_PENDING.name())) {
                     // 退款通知
                     orderNotificationService.sendOrderRefundRequestedNotification(
                             guest.getId(), cancelledOrder.getId(), cancelledOrder.getOrderNumber(),
                             reason != null && !reason.isEmpty() ? reason : "未提供原因",
                             RefundType.HOST_CANCELLED.toString(), // 需要根据实际情况确定退款类型
                             "0"); // 金额应该从订单中获取，这里暂时用0
                 } else {
                     // 取消通知
                     orderNotificationService.sendOrderCancelledNotification(
                             guest.getId(), cancelledOrder.getId(), cancelledOrder.getOrderNumber(),
                             cancelledOrder.getHomestay() != null ? cancelledOrder.getHomestay().getTitle() : "未知房源",
                             currentStatus.getDescription(),
                             OrderStatus.valueOf(cancelledOrder.getStatus()).getDescription(),
                             reason != null && !reason.isEmpty() ? reason : "未提供原因",
                             actor != null && actor.getId().equals(guest.getId())); // 是否由用户触发
                 }
                log.info("已为用户 {} 发送订单 {} 处理通知", guest.getUsername(), cancelledOrder.getOrderNumber());
            }
            // (可选) 如果也需要通知房东，请在此处添加类似逻辑

        } catch (Exception e) {
            log.error("发送订单处理通知失败: {}", e.getMessage(), e);
            // 通知发送失败不应影响订单处理流程
        }

        return convertToDTO(cancelledOrder);
    }

    @Override
    @Transactional
    public OrderDTO payOrder(Long id) {
        // 调用带支付方式的方法，使用默认支付方式
        return payOrder(id, "default");
    }

    @Override
    @Transactional
    public OrderDTO payOrder(Long id, String paymentMethod) {
        log.info("开始处理订单 ID: {} 的支付请求，支付方式: {}", id, paymentMethod);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        log.debug("找到订单: {}, 当前状态: {}", order.getOrderNumber(), order.getStatus());

        // 检查当前用户是否有权限
        User currentUser = getCurrentUser();
        log.debug("执行支付操作的用户: {}", currentUser.getUsername());
        if (!isOrderGuest(order, currentUser)) {
            log.warn("用户 {} 尝试支付不属于自己的订单 {}，操作被拒绝。", currentUser.getUsername(), order.getOrderNumber());
            throw new IllegalArgumentException("只有订单的客户可以支付订单");
        }

        // 检查订单状态
        OrderStatus currentStatus = OrderStatus.valueOf(order.getStatus());
        log.debug("检查订单 {} 的状态是否允许支付 (当前: {})", order.getOrderNumber(), currentStatus);
        if (currentStatus != OrderStatus.CONFIRMED && currentStatus != OrderStatus.PAYMENT_PENDING) {
            log.warn("订单 {} 状态为 {}，不允许支付。", order.getOrderNumber(), currentStatus);
            throw new IllegalArgumentException("只有已确认或正在支付中的订单可以支付");
        }

        // 模拟支付处理
        log.debug("开始模拟订单 {} 的支付处理...", order.getOrderNumber());
        try {
            // 模拟支付处理延迟
            Thread.sleep(500);
            log.debug("模拟支付处理完成，准备更新订单 {} 状态为 PAID。", order.getOrderNumber());

            // 支付成功，更新订单状态
            order.setStatus(OrderStatus.PAID.name());
            order.setPaymentStatus(PaymentStatus.PAID);

            // 添加支付备注
            String remark = (order.getRemark() != null && !order.getRemark().isEmpty()) ? order.getRemark() + "\n" : "";
            remark += "支付方式: " + paymentMethod;
            order.setRemark(remark);
            log.debug("更新订单 {} 备注: {}", order.getOrderNumber(), remark);

            Order paidOrder = orderRepository.save(order);
            log.info("订单 {} 状态已成功更新为 PAID 并保存。", paidOrder.getOrderNumber());

            // --- 发送支付成功通知给房东和房客 ---
            try {
                User host = paidOrder.getHomestay().getOwner();
                User guest = paidOrder.getGuest();
                String paymentMethod = "default"; // 从参数获取，这里简化处理
                orderNotificationService.sendOrderPaymentSuccessNotification(
                        host.getId(), guest.getId(), paidOrder.getId(),
                        host.getUsername(), guest.getUsername(),
                        paidOrder.getHomestay().getTitle(),
                        paidOrder.getOrderNumber(),
                        paymentMethod);
            } catch (Exception notifyEx) {
                log.error("发送订单 {} 支付成功通知失败: {}", paidOrder.getOrderNumber(), notifyEx.getMessage(), notifyEx);
            }
            // --- 通知发送结束 ---

            // --- 添加：自动生成待结算收益记录 ---
            log.info("订单 {} 支付成功，准备调用 EarningService 生成收益记录...", paidOrder.getOrderNumber());
            EarningDTO generatedEarning = null;
            try {
                // 调用重命名后的方法
                generatedEarning = earningService.generatePendingEarningForOrder(paidOrder.getId());
                if (generatedEarning != null) {
                    log.info("为订单 {} 成功生成或获取收益记录 ID: {}", paidOrder.getOrderNumber(), generatedEarning.getId());
                } else {
                    log.warn("订单 {} 的收益记录生成调用返回为空，可能已存在或生成失败（详见 EarningService 日志）。", paidOrder.getOrderNumber());
                }
            } catch (Exception earningEx) {
                // 记录收益生成失败的错误，但不应中断支付成功流程
                log.error("为订单 {} 调用 generatePendingEarningForOrder 时发生异常: {}", paidOrder.getOrderNumber(),
                        earningEx.getMessage(), earningEx);
                // 这里可以选择是否需要额外的错误处理，比如标记订单需要人工处理收益等
            }
            log.info("EarningService 调用结束，订单 {} 的支付流程完成。", paidOrder.getOrderNumber());
            // --- 收益生成结束 ---

            return convertToDTO(paidOrder);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单 {} 支付处理被中断", order.getOrderNumber(), e);
            throw new RuntimeException("支付处理中断");
        } catch (Exception e) {
            // 支付失败，更新订单状态为支付失败
            log.error("订单 {} 支付过程中发生意外错误，尝试将状态更新为 PAYMENT_FAILED: {}", order.getOrderNumber(), e.getMessage(), e);
            order.setStatus(OrderStatus.PAYMENT_FAILED.name());
            order.setPaymentStatus(PaymentStatus.UNPAID);
            orderRepository.save(order);
            // 支付失败不应该生成收益，也不需要发通知
            throw new RuntimeException("支付失败: " + e.getMessage());
        }
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

        // 假设清洁费为每晚价格的10%
        BigDecimal cleaningFee = pricePerNight.multiply(BigDecimal.valueOf(0.1));

        // 假设服务费为总价的15%
        BigDecimal serviceFee = baseAmount.multiply(BigDecimal.valueOf(0.15));

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
    public OrderDTO rejectOrder(Long id, String reason) {
        User currentUser = getCurrentUser(); // 房东
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查是否为房东
        if (!isOrderOwner(order, currentUser)) {
            throw new IllegalArgumentException("无权操作此订单");
        }

        // 检查订单状态是否为待确认
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("只能拒绝待确认状态的订单");
        }

        // 更新订单状态
        order.setStatus(OrderStatus.REJECTED.name());
        order.setRemark((order.getRemark() != null ? order.getRemark() + "\n" : "") + "拒绝原因: " + reason);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

            // --- 添加：发送预订拒绝通知给房客 ---
            try {
                orderNotificationService.sendOrderRejectedNotification(
                        order.getGuest().getId(),
                        currentUser.getId(),
                        order.getId(),
                        order.getGuest().getUsername(),
                        order.getHomestay().getTitle(),
                        order.getOrderNumber(),
                        reason);
            } catch (Exception e) {
                log.error("为房客 {} 发送订单 {} 被拒绝的通知失败: {}", order.getGuest().getUsername(), order.getOrderNumber(),
                        e.getMessage(), e);
            }
            // --- 通知发送结束 ---

        return convertToDTO(updatedOrder);
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
        log.info("管理员手动确认订单支付，ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));

        // 简单校验：只有未支付的订单才能被手动确认支付
        if (order.getPaymentStatus() != PaymentStatus.UNPAID) {
            log.warn("订单 {} 的支付状态为 {}，不能手动确认支付", id, order.getPaymentStatus());
            throw new IllegalStateException("只有未支付的订单才能被确认支付");
        }

        // 更新状态
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        // 可选：记录支付方式为手动确认
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isEmpty()) {
            order.setPaymentMethod("MANUAL_CONFIRM");
        }
        // 可选：更新支付时间
        // order.setPaymentTime(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        log.info("订单 {} 已手动确认支付", id);

        // 触发支付成功后的逻辑（如生成收益） - 使用正确的参数类型
        earningService.generatePendingEarningForOrder(updatedOrder.getId());

        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO initiateRefund(Long id /* , RefundRequest refundRequest */) {
        log.info("管理员发起退款流程，订单ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));

        // 简单校验：通常只有已支付的订单才能发起退款
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("订单 {} 的支付状态为 {}，不能发起退款", id, order.getPaymentStatus());
            throw new IllegalStateException("只有已支付的订单才能发起退款");
        }

        // 更新支付状态为退款中
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        // 可选：订单状态也可能需要调整，取决于业务逻辑
        // order.setStatus(OrderStatus.REFUND_PENDING.name());

        Order updatedOrder = orderRepository.save(order);
        log.info("订单 {} 已标记为退款中", id);

        // --- 此处应包含实际调用支付网关API发起退款的逻辑 ---
        // --- 以及处理退款结果回调的逻辑 ---

    // 发送通知等后续操作...
    try {
        orderNotificationService.sendOrderRefundInitiatedNotification(
                order.getGuest().getId(),
                order.getId(),
                order.getOrderNumber(),
                getCurrentUser().getId() // 触发者：发起退款的管理员
        );
    } catch (Exception e) {
        log.error("发送退款启动通知失败: {}", e.getMessage(), e);
        // 通知发送失败不应影响主业务流程
    }

        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO approveRefund(Long id, String refundNote) {
        log.info("管理员批准退款申请，订单ID: {}, 备注: {}", id, refundNote);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));

        // 检查当前状态
        if (order.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException("只有退款中的订单才能批准退款");
        }

        // 调用支付网关退款API
        try {
            RefundRequest refundRequest = RefundRequest.builder()
                    .orderId(order.getId())
                    .refundAmount(order.getRefundAmount() != null ? order.getRefundAmount() : order.getTotalAmount())
                    .refundReason(order.getRefundReason())
                    .refundType(order.getRefundType())
                    .paymentMethod(order.getPaymentMethod())
                    .build();

            RefundResponse refundResponse = paymentService.processRefund(refundRequest);

            if (!refundResponse.isSuccess()) {
                throw new RuntimeException("退款失败: " + refundResponse.getMessage());
            }

            // 更新退款交易号
            order.setRefundTransactionId(refundResponse.getRefundTradeNo());

            // 批准退款后直接标记为已退款
            order.setPaymentStatus(PaymentStatus.REFUNDED);

            // 如果订单状态也是退款中，更新为已退款
            if (OrderStatus.REFUND_PENDING.name().equals(order.getStatus())) {
                order.setStatus(OrderStatus.REFUNDED.name());
            }

            // 记录退款处理信息
            User currentUser = getCurrentUser();
            order.setRefundProcessedBy(currentUser.getId());
            order.setRefundProcessedAt(LocalDateTime.now());

            log.info("退款申请已批准并完成，订单 {} 状态更新为已退款，退款交易号: {}",
                    order.getOrderNumber(), refundResponse.getRefundTradeNo());

            // 添加批准备注
            String approvalNote = String.format("退款申请已批准并完成 - 管理员: %s, 备注: %s",
                    currentUser.getUsername(),
                    refundNote != null && !refundNote.isEmpty() ? refundNote : "无");
            if (order.getRemark() != null && !order.getRemark().isEmpty()) {
                order.setRemark(order.getRemark() + "\n" + approvalNote);
            } else {
                order.setRemark(approvalNote);
            }

            Order updatedOrder = orderRepository.save(order);

            // 发送退款批准通知
            try {
                orderNotificationService.sendOrderRefundApprovedNotification(
                        order.getGuest().getId(),
                        order.getId(),
                        order.getOrderNumber(),
                        refundNote);
            } catch (Exception e) {
                log.error("发送退款批准通知失败: {}", e.getMessage(), e);
            }
            } catch (Exception e) {
                log.error("发送退款批准通知失败: {}", e.getMessage(), e);
            }

            return convertToDTO(updatedOrder);

        } catch (Exception e) {
            log.error("批准退款失败，订单ID: {}", id, e);
            throw new RuntimeException("批准退款失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderDTO rejectRefund(Long id, String rejectReason) {
        log.info("管理员拒绝退款申请，订单ID: {}, 原因: {}", id, rejectReason);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));

        // 检查当前状态
        if (order.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException("只有退款中的订单才能拒绝退款");
        }

        // 拒绝退款，恢复为已支付状态
        order.setPaymentStatus(PaymentStatus.PAID);

        // 如果订单状态也是退款中，也需要恢复
        if (OrderStatus.REFUND_PENDING.name().equals(order.getStatus())) {
            order.setStatus(OrderStatus.PAID.name());
        }

        // 保存拒绝原因到专有字段（用于前端展示）
        order.setRefundRejectionReason(
            rejectReason != null && !rejectReason.isEmpty() ? rejectReason : "未提供原因");

        // 添加拒绝原因到备注
        String rejectionNote = String.format("退款申请被拒绝 - 原因: %s",
                rejectReason != null && !rejectReason.isEmpty() ? rejectReason : "未提供原因");
        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            order.setRemark(order.getRemark() + "\n" + rejectionNote);
        } else {
            order.setRemark(rejectionNote);
        }

        Order updatedOrder = orderRepository.save(order);

            // 发送退款拒绝通知
            try {
                orderNotificationService.sendOrderRefundRejectedNotification(
                        order.getGuest().getId(),
                        order.getId(),
                        order.getOrderNumber(),
                        rejectReason);
            } catch (Exception e) {
                log.error("发送退款拒绝通知失败: {}", e.getMessage(), e);
            }

        log.info("订单 {} 退款申请已被拒绝", order.getOrderNumber());
        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO completeRefund(Long id, String refundTransactionId) {
        log.info("管理员完成退款处理，订单ID: {}, 退款交易号: {}", id, refundTransactionId);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));

        // 检查当前状态
        if (order.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException("只有退款中的订单才能完成退款");
        }

        // 标记退款完成
        order.setPaymentStatus(PaymentStatus.REFUNDED);

        // 如果订单状态也是退款中，更新为已退款
        if (OrderStatus.REFUND_PENDING.name().equals(order.getStatus())) {
            order.setStatus(OrderStatus.REFUNDED.name());
        }

        // 记录退款处理信息
        User currentUser = getCurrentUser();
        order.setRefundProcessedBy(currentUser.getId());
        order.setRefundProcessedAt(LocalDateTime.now());
        order.setRefundTransactionId(refundTransactionId);

        // 添加退款完成信息到备注
        String completionNote = String.format("退款已完成 - 处理人: %s, 交易号: %s",
                currentUser.getUsername(),
                refundTransactionId != null && !refundTransactionId.isEmpty() ? refundTransactionId : "无");
        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            order.setRemark(order.getRemark() + "\n" + completionNote);
        } else {
            order.setRemark(completionNote);
        }

        Order updatedOrder = orderRepository.save(order);

            // 发送退款完成通知
            try {
                orderNotificationService.sendOrderRefundCompletedNotification(
                        order.getGuest().getId(),
                        order.getId(),
                        order.getOrderNumber(),
                        refundTransactionId);
            } catch (Exception e) {
                log.error("发送退款完成通知失败: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("发送退款完成通知失败: {}", e.getMessage(), e);
        }

        log.info("订单 {} 退款已完成", order.getOrderNumber());
        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO requestUserRefund(Long id, String reason) {
        log.info("用户申请退款，订单ID: {}, 退款原因: {}", id, reason);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));

        // 检查当前状态是否允许申请退款
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("只有已支付的订单才能申请退款");
        }

        // 检查是否已经在退款流程中
        if (order.getStatus().equals(OrderStatus.REFUND_PENDING.name()) ||
                order.getStatus().equals(OrderStatus.REFUNDED.name())) {
            throw new IllegalStateException("订单已在退款流程中，请勿重复申请");
        }

        // 更新订单状态为退款中
        order.setStatus(OrderStatus.REFUND_PENDING.name());
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);

        // 设置退款相关信息
        User currentUser = getCurrentUser();
        order.setRefundType(RefundType.USER_REQUESTED);
        order.setRefundReason(reason != null && !reason.isEmpty() ? reason : "用户申请退款");

        // 使用新逻辑计算退款金额
        BigDecimal calculatedRefund = calculateRefundAmount(order);
        order.setRefundAmount(calculatedRefund);

        order.setRefundInitiatedBy(currentUser.getId());
        order.setRefundInitiatedAt(LocalDateTime.now());

        // 添加退款申请记录到备注
        String refundNote = String.format("退款申请 - 类型: %s, 原因: %s, 申请人: %s, 计算退款金额: %s",
                RefundType.USER_REQUESTED.getDescription(),
                reason != null && !reason.isEmpty() ? reason : "用户申请退款",
                currentUser.getUsername(),
                calculatedRefund);
        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            order.setRemark(order.getRemark() + "\n" + refundNote);
        } else {
            order.setRemark(refundNote);
        }

        Order updatedOrder = orderRepository.save(order);

        // 发送退款申请通知给管理员
        try {
            // 这里可以发送通知给管理员，告知有新的退款申请
            log.info("退款申请已提交，订单号: {}, 等待管理员处理", order.getOrderNumber());
        } catch (Exception e) {
            log.error("发送退款申请通知失败: {}", e.getMessage(), e);
        }

        log.info("用户退款申请已提交，订单号: {}", order.getOrderNumber());
        return convertToDTO(updatedOrder);
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

        // 获取房东信息，如果存在的话
        User host = order.getHomestay() != null ? order.getHomestay().getOwner() : null;
        String hostName = host != null ? (host.getNickname() != null ? host.getNickname() : host.getUsername()) : null;
        Long hostId = host != null ? host.getId() : null;

        // 获取房客信息，如果存在的话
        User guest = order.getGuest();
        String guestName = guest != null ? (guest.getNickname() != null ? guest.getNickname() : guest.getUsername())
                : null;
        Long guestId = guest != null ? guest.getId() : null;

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

        // 计算明细费用
        BigDecimal baseAmount = order.getPrice() != null ? order.getPrice().multiply(BigDecimal.valueOf(order.getNights())) : BigDecimal.ZERO;
        BigDecimal cleaningFee = order.getPrice() != null ? order.getPrice().multiply(BigDecimal.valueOf(0.1)) : BigDecimal.ZERO;
        BigDecimal serviceFee = baseAmount.multiply(BigDecimal.valueOf(0.15));

        // 构建 OrderDTO
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .homestayId(order.getHomestay() != null ? order.getHomestay().getId() : null)
                .homestayTitle(order.getHomestay() != null ? order.getHomestay().getTitle() : null)
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
}