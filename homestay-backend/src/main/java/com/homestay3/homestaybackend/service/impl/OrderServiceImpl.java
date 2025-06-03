package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.dto.EarningDTO;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
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
    private final EarningService earningService;
    private final ReviewRepository reviewRepository;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    
    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        log.info("开始创建订单，请求信息: {}", orderDTO);
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        log.info("当前下单用户: id={}, username={}", currentUser.getId(), currentUser.getUsername());
        
        // 获取房源信息
        Homestay homestay = homestayRepository.findById(orderDTO.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        log.info("关联房源信息: id={}, title={}, 房东ID: {}", homestay.getId(), homestay.getTitle(), homestay.getOwner().getId());
        
        // 检查日期是否有效
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
        
        // 计算总价
        BigDecimal totalAmount = homestay.getPrice().multiply(BigDecimal.valueOf(nights));
        log.info("计算订单总价: {} ({} 晚 x ￥{})", totalAmount, nights, homestay.getPrice());
        
        // 创建订单
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
                .status(OrderStatus.PENDING.name())
                .remark(orderDTO.getRemark())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("订单创建成功: id={}, orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());
        
        // --- 添加：发送新预订请求通知给房东 ---
        try {
            User host = homestay.getOwner(); // 获取房东用户对象
            String notificationContent = String.format(
                "您收到了来自用户 %s 的新预订请求 (订单 %s)，请及时处理。",
                currentUser.getUsername(), // 房客用户名
                savedOrder.getOrderNumber() // 订单号
            );

            notificationService.createNotification(
                    host.getId(),             // 接收者: 房东
                    currentUser.getId(),      // 触发者: 房客
                    NotificationType.BOOKING_REQUEST, // 类型: 新预订请求
                    EntityType.BOOKING,       // 关联实体类型: 预订
                    String.valueOf(savedOrder.getId()), // 关联实体ID: 订单ID
                    notificationContent         // 内容
            );
            log.info("已为房东 {} (民宿 {}) 发送新预订请求通知 (订单 {})", host.getUsername(), homestay.getTitle(), savedOrder.getOrderNumber());
        } catch (Exception e) {
            log.error("为房东 {} 发送新预订请求通知失败 (订单 {}): {}", homestay.getOwner().getUsername(), savedOrder.getOrderNumber(), e.getMessage(), e);
            // 发送通知失败不应中断订单创建流程
        }
        // --- 通知发送结束 ---
        
        // 设置初始支付状态
        savedOrder.setPaymentStatus(PaymentStatus.UNPAID);
        savedOrder = orderRepository.save(savedOrder); // 保存更新后的状态
        
        return convertToDTO(savedOrder);
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
                                cb.lessThanOrEqualTo(root.get("checkInDate"), endDate)
                        ),
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("checkOutDate"), startDate),
                                cb.lessThanOrEqualTo(root.get("checkOutDate"), endDate)
                        )
                ));
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
                String notificationContent = String.format(
                        "您的订单 %s (房源: %s) 已被房东 %s 确认，请及时支付。",
                        order.getOrderNumber(),
                        order.getHomestay().getTitle(),
                        currentUser.getUsername()
                );
                notificationService.createNotification(
                        guest.getId(),               // 接收者: 房客
                        currentUser.getId(),         // 触发者: 房东
                        NotificationType.ORDER_CONFIRMED, // 类型: 订单已确认
                        EntityType.ORDER,            // 关联实体类型: 订单
                        String.valueOf(order.getId()), // 关联实体ID: 订单ID
                        notificationContent          // 内容
                );
                log.info("已为房客 {} 发送订单 {} 确认通知", guest.getUsername(), order.getOrderNumber());
            } catch (Exception e) {
                log.error("为房客 {} 发送订单 {} 确认通知失败: {}", order.getGuest().getUsername(), order.getOrderNumber(), e.getMessage(), e);
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
                 User guest = order.getGuest();
                 User host = order.getHomestay().getOwner();
                 String homestayTitle = order.getHomestay().getTitle();
                 String orderNumber = order.getOrderNumber();
                 Long entityId = order.getId();

                 String contentBase = String.format("订单 %s (房源: %s) 已完成", orderNumber, homestayTitle);
                 String contentForGuest = contentBase + "。感谢您的入住！期待您再次光临。";
                 String contentForHost = contentBase + "。请尽快结算相关收益。";

                 // 获取触发操作的用户ID，如果无法获取则为null
                 Long triggerUserId = null;
                 try {
                     triggerUserId = currentUser.getId();
                 } catch (Exception ignored) {}

                 // 通知房客
                 notificationService.createNotification(
                         guest.getId(), triggerUserId, NotificationType.ORDER_COMPLETED, EntityType.ORDER, String.valueOf(entityId), contentForGuest
                 );
                 log.info("已为房客 {} 发送订单 {} 完成通知", guest.getUsername(), orderNumber);

                 // 通知房东
                 notificationService.createNotification(
                         host.getId(), triggerUserId, NotificationType.ORDER_COMPLETED, EntityType.ORDER, String.valueOf(entityId), contentForHost
                 );
                 log.info("已为房东 {} 发送订单 {} 完成通知", host.getUsername(), orderNumber);

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
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
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
            OrderStatus.REJECTED
        ));
        
        // 已确认状态可以转换为：支付中、已取消、已支付
        transitions.put(OrderStatus.CONFIRMED, Arrays.asList(
            OrderStatus.PAYMENT_PENDING, 
            OrderStatus.CANCELLED, 
            OrderStatus.CANCELLED_BY_USER,
            OrderStatus.CANCELLED_BY_HOST,
            OrderStatus.CANCELLED_SYSTEM,
            OrderStatus.PAID // 直接支付也可以
        ));
        
        // 支付中状态可以转换为：已支付、支付失败、用户取消
        transitions.put(OrderStatus.PAYMENT_PENDING, Arrays.asList(
            OrderStatus.PAID,
            OrderStatus.PAYMENT_FAILED,
            OrderStatus.CANCELLED_BY_USER,
            OrderStatus.CANCELLED // 系统取消
        ));
        
        // 支付失败状态可以转换为：支付中、已取消、用户取消
        transitions.put(OrderStatus.PAYMENT_FAILED, Arrays.asList(
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.CANCELLED,
            OrderStatus.CANCELLED_BY_USER
        ));
        
        // 已支付状态可以转换为：待入住、退款中、房东取消、已入住
        transitions.put(OrderStatus.PAID, Arrays.asList(
            OrderStatus.READY_FOR_CHECKIN,
            OrderStatus.REFUND_PENDING,
            OrderStatus.CANCELLED_BY_HOST,
            OrderStatus.CHECKED_IN // 直接入住也可以
        ));
        
        // 待入住状态可以转换为：已入住、退款中、房东取消
        transitions.put(OrderStatus.READY_FOR_CHECKIN, Arrays.asList(
            OrderStatus.CHECKED_IN,
            OrderStatus.REFUND_PENDING,
            OrderStatus.CANCELLED_BY_HOST
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
            OrderStatus.REFUND_FAILED
        ));
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
        
        // 验证状态流转是否合法
        if (!isValidStatusTransition(currentStatus, targetStatus)) {
            throw new IllegalArgumentException(
                String.format("不允许从当前状态 [%s] 取消订单", currentStatus.getDescription())
            );
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
        
        Order cancelledOrder = orderRepository.save(order);

        // --- 添加：发送订单取消通知 ---
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
                String notificationContent = String.format(
                        "您的订单 %s (房源: %s) 已被取消。状态从 %s 变为 %s。原因: %s",
                        cancelledOrder.getOrderNumber(),
                        cancelledOrder.getHomestay() != null ? cancelledOrder.getHomestay().getTitle() : "未知房源",
                        currentStatus.getDescription(), // 使用 currentStatus 的描述
                        targetStatus.getDescription(),  // 使用 targetStatus 的描述
                        reason != null && !reason.isEmpty() ? reason : "未提供原因"
                );

                notificationService.createNotification(
                        guest.getId(),
                        actor != null ? actor.getId() : null,
                        NotificationType.ORDER_STATUS_CHANGED, // 或更具体的如 ORDER_CANCELLED
                        EntityType.ORDER,
                        String.valueOf(cancelledOrder.getId()),
                        notificationContent
                );
                log.info("已为用户 {} 发送订单 {} 取消通知", guest.getUsername(), cancelledOrder.getOrderNumber());
            }
             // (可选) 如果也需要通知房东，请在此处添加类似逻辑

        } catch (Exception e) {
            log.error("发送订单取消通知失败: {}", e.getMessage(), e);
            // 通知发送失败不应影响订单取消流程
        }
        
        // --- 添加：如果是已支付订单被取消，需要触发退款流程 ---
        if (cancelledOrder.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("已支付订单被取消，需要执行退款流程，订单号: {}", cancelledOrder.getOrderNumber());
            try {
                // 标记退款状态
                cancelledOrder.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                cancelledOrder = orderRepository.save(cancelledOrder);
                
                // 如果是房东取消，根据业务规则可能需要罚金
                if (targetStatus == OrderStatus.CANCELLED_BY_HOST) {
                    log.info("房东取消已支付订单，可能需要处理罚金，订单号: {}", cancelledOrder.getOrderNumber());
                    // 这里可以添加罚金处理逻辑
                }
                
                // 实际触发退款可以是异步的或者需要管理员手动处理
                // initiateRefund(cancelledOrder.getId());
            } catch (Exception e) {
                log.error("标记订单退款状态失败: {}", e.getMessage(), e);
                // 我们不想因为退款标记失败而导致整个取消过程失败
                // 此处可以发送告警让管理员手动处理
            }
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
            
            // --- 发送支付成功通知给房东 (保留) ---
            User host = paidOrder.getHomestay().getOwner();
            User guest = paidOrder.getGuest(); // 获取房客信息
            try {
                // User guest = paidOrder.getGuest(); // guest 变量在此处未使用，注释掉 // 移除注释
                String notificationContentForHost = String.format(
                        "用户 %s 已支付订单 %s (房源: %s)。",
                        guest.getUsername(), // 使用获取到的房客信息
                        paidOrder.getOrderNumber(),
                        paidOrder.getHomestay().getTitle()
                );
                notificationService.createNotification(
                        host.getId(),                // 接收者: 房东
                        guest.getId(),               // 触发者: 房客 (支付操作者)
                        NotificationType.PAYMENT_RECEIVED,
                        EntityType.ORDER,
                        String.valueOf(paidOrder.getId()),
                        notificationContentForHost
                );
                log.info("已为房东 {} 发送订单 {} 支付成功通知", host.getUsername(), paidOrder.getOrderNumber());
            } catch (Exception notifyEx) {
                log.error("为房东 {} 发送订单 {} 支付成功通知失败: {}", paidOrder.getHomestay().getOwner().getUsername(), paidOrder.getOrderNumber(), notifyEx.getMessage(), notifyEx);
            }
            // --- 房东通知发送结束 ---

            // --- 添加：发送预订接受通知给房客 ---
            try {
                String notificationContentForGuest = String.format(
                        "您的订单 %s (房源: %s) 已被房东确认并支付成功！",
                        paidOrder.getOrderNumber(),
                        paidOrder.getHomestay().getTitle()
                );
                notificationService.createNotification(
                        guest.getId(),               // 接收者: 房客
                        host.getId(),                // 触发者: 房东 (或系统，这里用房东更合理，代表房东接受了这次预订)
                        NotificationType.BOOKING_ACCEPTED, // 类型: 预订已接受
                        EntityType.ORDER,            // 关联实体类型
                        String.valueOf(paidOrder.getId()), // 关联实体ID
                        notificationContentForGuest  // 内容
                );
                log.info("已为房客 {} 发送订单 {} 被接受的通知", guest.getUsername(), paidOrder.getOrderNumber());
            } catch (Exception notifyEx) {
                log.error("为房客 {} 发送订单 {} 被接受的通知失败: {}", guest.getUsername(), paidOrder.getOrderNumber(), notifyEx.getMessage(), notifyEx);
            }
            // --- 房客通知发送结束 ---


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
                log.error("为订单 {} 调用 generatePendingEarningForOrder 时发生异常: {}", paidOrder.getOrderNumber(), earningEx.getMessage(), earningEx);
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
            User guest = order.getGuest();
            String homestayTitle = order.getHomestay().getTitle();
            String orderNumber = order.getOrderNumber();
            String notificationContent = String.format(
                    "很抱歉，您关于房源 '%s' 的预订请求 (订单 %s) 已被房东拒绝。原因: %s",
                    homestayTitle,
                    orderNumber,
                    (reason != null && !reason.isEmpty()) ? reason : "未提供具体原因"
            );
            notificationService.createNotification(
                    guest.getId(),             // 接收者: 房客
                    currentUser.getId(),       // 触发者: 房东 (当前用户)
                    NotificationType.BOOKING_REJECTED,
                    EntityType.ORDER,
                    String.valueOf(updatedOrder.getId()),
                    notificationContent
            );
            log.info("已为房客 {} 发送订单 {} 被拒绝的通知", guest.getUsername(), orderNumber);
        } catch (Exception notifyEx) {
            log.error("为房客 {} 发送订单 {} 被拒绝的通知失败: {}", order.getGuest().getUsername(), order.getOrderNumber(), notifyEx.getMessage(), notifyEx);
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
            LocalDate createTimeEnd
    ) {
        log.info("Admin获取订单列表，筛选条件: orderNumber={}, guestName={}, homestayTitle={}, status={}, paymentStatus={}, paymentMethod={}, hostName={}, checkInDateStart={}, checkInDateEnd={}, createTimeStart={}, createTimeEnd={}",
                orderNumber, guestName, homestayTitle, status, paymentStatus, paymentMethod, hostName, checkInDateStart, checkInDateEnd, createTimeStart, createTimeEnd);

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
                if (homestayJoin == null) homestayJoin = root.join("homestay");
                predicates.add(cb.like(homestayJoin.get("title"), "%" + homestayTitle + "%"));
            }

            // 房东姓名/用户名
            if (hostName != null && !hostName.isEmpty()) {
                if (homestayJoin == null) homestayJoin = root.join("homestay");
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
    public OrderDTO initiateRefund(Long id /*, RefundRequest refundRequest */) {
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
        // notificationService.createNotification(...);
        
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
        return order.getHomestay() != null && order.getHomestay().getOwner() != null && order.getHomestay().getOwner().getId().equals(user.getId());
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
     * @param order 订单对象
     * @param user 当前用户
     * @return 如果用户有权访问此订单则返回true
     */
    private boolean isOrderAccessible(Order order, User user) {
        // 添加日志输出，便于调试
        System.out.println("检查订单访问权限 - 订单ID: " + order.getId() + 
                          ", 用户: " + user.getUsername() + 
                          ", 角色: " + user.getRole());
        System.out.println("订单所属房东ID: " + order.getHomestay().getOwner().getId() + 
                          ", 当前用户ID: " + user.getId());
        System.out.println("订单客人ID: " + order.getGuest().getId());
        
        // 检查用户角色 - 考虑可能有前缀或不一致的情况
        boolean isAdmin = user.getRole().contains("ADMIN");
        
        // 管理员可以访问所有订单
        if (isAdmin) {
            System.out.println("用户是管理员，允许访问");
            return true;
        }
        
        // 房东可以访问自己房源的订单
        boolean isHost = user.getRole().contains("HOST");
        if (isHost) {
            boolean isOwner = isOrderOwner(order, user);
            System.out.println("用户是房东，是否为订单所属房东: " + isOwner);
            if (isOwner) {
                return true;
            }
        }
        
        // 客户可以访问自己的订单
        boolean isUser = user.getRole().contains("USER");
        if (isUser) {
            boolean isGuest = isOrderGuest(order, user);
            System.out.println("用户是普通用户，是否为订单客人: " + isGuest);
            if (isGuest) {
                return true;
            }
        }
        
        System.out.println("权限检查失败，拒绝访问");
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
        String guestName = guest != null ? (guest.getNickname() != null ? guest.getNickname() : guest.getUsername()) : null;
        Long guestId = guest != null ? guest.getId() : null;
        
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
                .build();
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