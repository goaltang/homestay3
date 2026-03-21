package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.CheckOutDTO;
import com.homestay3.homestaybackend.entity.CheckOutRecord;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.CheckOutRecordRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.CheckOutService;
import com.homestay3.homestaybackend.service.EarningService;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 退房服务实现
 */
@Service
@RequiredArgsConstructor
public class CheckOutServiceImpl implements CheckOutService {

    private static final Logger log = LoggerFactory.getLogger(CheckOutServiceImpl.class);
    private static final String AUTO_CHECKOUT_TIME_KEY = "AUTO_CHECKOUT_TIME";
    private static final String DEFAULT_AUTO_CHECKOUT_TIME = "12:00";
    private static final String CLEANING_FEE_RATE_KEY = "CLEANING_FEE_RATE";
    private static final String SERVICE_FEE_RATE_KEY = "SERVICE_FEE_RATE";
    private static final BigDecimal DEFAULT_CLEANING_FEE_RATE = new BigDecimal("0.10");
    private static final BigDecimal DEFAULT_SERVICE_FEE_RATE = new BigDecimal("0.15");

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CheckOutRecordRepository checkOutRecordRepository;
    private final SystemConfigService systemConfigService;
    private final NotificationService notificationService;
    private final EarningService earningService;

    @Override
    @Transactional
    public CheckOutDTO performCheckOut(Long orderId, CheckOutDTO checkOutDTO) {
        log.info("办理退房 - 订单ID: {}", orderId);

        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：房东、管理员或客人
        if (!isOrderAccessible(order, currentUser)) {
            throw new AccessDeniedException("无权操作此订单");
        }

        // 状态检查：必须是 CHECKED_IN
        if (!OrderStatus.CHECKED_IN.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不是已入住状态");
        }

        LocalDateTime now = LocalDateTime.now();

        // 计算实际住宿天数（使用实际入住时间，而非计划入住日期）
        LocalDate actualCheckInDate = order.getCheckedInAt() != null
                ? order.getCheckedInAt().toLocalDate()
                : order.getCheckInDate();
        int actualNights = calculateActualNights(actualCheckInDate, now.toLocalDate());
        if (actualNights < 1) {
            actualNights = 1;
        }

        // 获取押金
        BigDecimal depositAmount = order.getDepositAmount();

        // 计算结算金额
        BigDecimal settlementAmount = calculateSettlementAmount(order, actualNights, depositAmount);

        // 确定退房方式
        String checkOutMethod = determineCheckOutMethod(currentUser, order);

        // 创建退房记录
        CheckOutRecord record = CheckOutRecord.builder()
                .orderId(orderId)
                .checkOutMethod(checkOutMethod)
                .checkedOutAt(now)
                .checkOutOperatorId(currentUser.getId())
                .checkOutOperatorType(getOperatorType(currentUser, order))
                .actualNights(actualNights)
                .depositAmount(depositAmount)
                .depositStatus(depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0 ? "PENDING" : "WAIVED")
                .settlementAmount(settlementAmount)
                .remark(checkOutDTO != null ? checkOutDTO.getRemark() : null)
                .status("PENDING")
                .build();
        checkOutRecordRepository.save(record);

        // 更新订单状态
        order.setStatus(OrderStatus.CHECKED_OUT.name());
        order.setCheckedOutAt(now);
        orderRepository.save(order);

        // 发送退房通知
        sendCheckOutNotification(order, settlementAmount);

        log.info("订单 {} 已办理退房，实际住宿 {} 晚，结算金额: {}", order.getOrderNumber(), actualNights, settlementAmount);
        return convertToDTO(record, order);
    }

    @Override
    @Transactional
    public CheckOutDTO selfCheckOut(Long orderId) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：只有客人可以自助退房
        if (!isOrderGuest(order, currentUser)) {
            throw new AccessDeniedException("只有入住人可以自助退房");
        }

        // 状态检查：必须是 CHECKED_IN
        if (!OrderStatus.CHECKED_IN.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不是已入住状态");
        }

        // 调用 performCheckOut
        return performCheckOut(orderId, CheckOutDTO.builder().build());
    }

    @Override
    @Transactional
    public CheckOutDTO processDeposit(Long orderId, String action, BigDecimal amount, String note) {
        log.info("处理押金 - 订单ID: {}, 操作: {}, 金额: {}", orderId, action, amount);

        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：只有房东可以处理押金
        if (!isOrderOwner(order, currentUser)) {
            throw new AccessDeniedException("只有房东才能处理押金");
        }

        CheckOutRecord record = checkOutRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("退房记录不存在"));

        switch (action.toUpperCase()) {
            case "COLLECT":
                // 收取押金
                record.setDepositAmount(amount);
                record.setDepositStatus("PAID");
                if (order.getDepositAmount() == null) {
                    order.setDepositAmount(amount);
                    orderRepository.save(order);
                }
                break;
            case "REFUND":
                // 退还押金
                record.setDepositStatus("REFUNDED");
                // 清除订单上的押金金额
                order.setDepositAmount(BigDecimal.ZERO);
                orderRepository.save(order);
                // TODO: 调用支付网关实际退款
                break;
            case "RETAIN":
                // 扣押押金
                record.setDepositStatus("RETAINED");
                if (record.getExtraCharges() == null) {
                    record.setExtraCharges(amount);
                    record.setExtraChargesDescription(note);
                } else {
                    record.setExtraCharges(record.getExtraCharges().add(amount));
                    record.setExtraChargesDescription((record.getExtraChargesDescription() != null ? record.getExtraChargesDescription() + "; " : "") + note);
                }
                // 重新计算结算金额
                recalculateSettlementAmount(record, order);
                break;
            case "WAIVE":
                // 免除押金
                record.setDepositStatus("WAIVED");
                record.setDepositAmount(BigDecimal.ZERO);
                recalculateSettlementAmount(record, order);
                break;
            default:
                throw new IllegalArgumentException("无效的押金操作: " + action);
        }

        if (note != null && !note.isEmpty()) {
            String existingRemark = record.getRemark();
            record.setRemark((existingRemark != null ? existingRemark + "; " : "") + "押金操作: " + action + " - " + note);
        }

        checkOutRecordRepository.save(record);
        log.info("押金操作完成 - 订单: {}, 操作: {}, 状态: {}", order.getOrderNumber(), action, record.getDepositStatus());
        return convertToDTO(record, order);
    }

    @Override
    public CheckOutDTO getCheckOutRecord(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        CheckOutRecord record = checkOutRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("退房记录不存在"));

        return convertToDTO(record, order);
    }

    @Override
    @Transactional
    public CheckOutDTO confirmSettlement(Long orderId) {
        log.info("确认结算 - 订单ID: {}", orderId);

        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：房东或管理员
        if (!isOrderOwner(order, currentUser) && !hasAdminAuthority(currentUser)) {
            throw new AccessDeniedException("无权确认结算");
        }

        // 状态检查：必须是 CHECKED_OUT
        if (!OrderStatus.CHECKED_OUT.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不是已退房状态");
        }

        CheckOutRecord record = checkOutRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("退房记录不存在"));

        // 押金状态校验：押金未处理时不允许结算
        String depositStatus = record.getDepositStatus();
        if ("PENDING".equals(depositStatus) && record.getDepositAmount() != null
                && record.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("押金尚未处理完毕，请先完成押金操作（收取/退还/扣押/免除）");
        }

        // 生成待结算收益
        try {
            earningService.generatePendingEarningForOrder(orderId);
        } catch (Exception e) {
            log.error("生成待结算收益失败: {}", e.getMessage(), e);
        }

        // 更新退房记录状态
        record.setStatus("COMPLETED");
        checkOutRecordRepository.save(record);

        // 更新订单状态
        order.setStatus(OrderStatus.COMPLETED.name());
        orderRepository.save(order);

        // 发送结算完成通知
        sendSettlementCompletedNotification(order);

        log.info("订单 {} 结算完成", order.getOrderNumber());
        return convertToDTO(record, order);
    }

    @Override
    @Transactional
    public CheckOutDTO updateExtraCharges(Long orderId, BigDecimal extraCharges, String description) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：只有房东可以更新额外费用
        if (!isOrderOwner(order, currentUser)) {
            throw new AccessDeniedException("只有房东才能更新额外费用");
        }

        CheckOutRecord record = checkOutRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("退房记录不存在"));

        record.setExtraCharges(extraCharges);
        record.setExtraChargesDescription(description);
        recalculateSettlementAmount(record, order);
        checkOutRecordRepository.save(record);

        return convertToDTO(record, order);
    }

    @Override
    public void validateAccess(Long orderId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        if (!isOrderAccessible(order, user)) {
            throw new AccessDeniedException("无权访问此订单的退房信息");
        }
    }

    // ==================== 私有方法 ====================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    private boolean isOrderOwner(Order order, User user) {
        return order.getHomestay() != null && order.getHomestay().getOwner() != null
                && order.getHomestay().getOwner().getId().equals(user.getId());
    }

    private boolean isOrderGuest(Order order, User user) {
        return order.getGuest() != null && order.getGuest().getId().equals(user.getId());
    }

    private boolean hasAdminAuthority(User user) {
        return user.getRole() != null && user.getRole().contains("ADMIN");
    }

    private boolean isOrderAccessible(Order order, User user) {
        return isOrderOwner(order, user) || isOrderGuest(order, user) || hasAdminAuthority(user);
    }

    private int calculateActualNights(java.time.LocalDate checkInDate, java.time.LocalDate actualCheckOutDate) {
        return (int) ChronoUnit.DAYS.between(checkInDate, actualCheckOutDate);
    }

    private BigDecimal getCleaningFeeRate() {
        String rate = systemConfigService.getConfigValue(CLEANING_FEE_RATE_KEY);
        if (rate != null && !rate.isEmpty()) {
            return new BigDecimal(rate);
        }
        return DEFAULT_CLEANING_FEE_RATE;
    }

    private BigDecimal getServiceFeeRate() {
        String rate = systemConfigService.getConfigValue(SERVICE_FEE_RATE_KEY);
        if (rate != null && !rate.isEmpty()) {
            return new BigDecimal(rate);
        }
        return DEFAULT_SERVICE_FEE_RATE;
    }

    private BigDecimal calculateSettlementAmount(Order order, int actualNights, BigDecimal depositAmount) {
        // 计算房费
        BigDecimal baseAmount = order.getPrice().multiply(BigDecimal.valueOf(actualNights));
        // 清洁费（从配置读取，默认10%）
        BigDecimal cleaningFee = baseAmount.multiply(getCleaningFeeRate());
        // 服务费（从配置读取，默认15%）
        BigDecimal serviceFee = baseAmount.multiply(getServiceFeeRate());

        BigDecimal totalFee = baseAmount.add(cleaningFee).add(serviceFee);

        // 减去押金
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalFee.subtract(depositAmount).setScale(2, RoundingMode.HALF_UP);
        }

        return totalFee.setScale(2, RoundingMode.HALF_UP);
    }

    private void recalculateSettlementAmount(CheckOutRecord record, Order order) {
        int actualNights = record.getActualNights() != null ? record.getActualNights() : 1;
        BigDecimal baseAmount = order.getPrice().multiply(BigDecimal.valueOf(actualNights));
        BigDecimal cleaningFee = baseAmount.multiply(getCleaningFeeRate());
        BigDecimal serviceFee = baseAmount.multiply(getServiceFeeRate());
        BigDecimal totalFee = baseAmount.add(cleaningFee).add(serviceFee);

        // 加上额外费用
        if (record.getExtraCharges() != null) {
            totalFee = totalFee.add(record.getExtraCharges());
        }

        // 减去押金
        if (record.getDepositAmount() != null && record.getDepositAmount().compareTo(BigDecimal.ZERO) > 0
                && "REFUNDED".equals(record.getDepositStatus())) {
            totalFee = totalFee.subtract(record.getDepositAmount());
        }

        record.setSettlementAmount(totalFee.setScale(2, RoundingMode.HALF_UP));
    }

    private String determineCheckOutMethod(User user, Order order) {
        if (isOrderGuest(order, user)) {
            return "SELF_SERVICE";
        } else if (hasAdminAuthority(user)) {
            return "MANUAL";
        } else {
            return "MANUAL";
        }
    }

    private String getOperatorType(User user, Order order) {
        if (hasAdminAuthority(user)) {
            return "ADMIN";
        } else if (isOrderOwner(order, user)) {
            return "HOST";
        } else if (isOrderGuest(order, user)) {
            return "GUEST";
        }
        return "UNKNOWN";
    }

    private CheckOutDTO convertToDTO(CheckOutRecord record, Order order) {
        return CheckOutDTO.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .checkOutMethod(record.getCheckOutMethod())
                .checkedOutAt(record.getCheckedOutAt())
                .operatorType(record.getCheckOutOperatorType())
                .actualNights(record.getActualNights())
                .depositAmount(record.getDepositAmount())
                .depositStatus(record.getDepositStatus())
                .extraCharges(record.getExtraCharges())
                .extraChargesDescription(record.getExtraChargesDescription())
                .settlementAmount(record.getSettlementAmount())
                .status(record.getStatus())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private void sendCheckOutNotification(Order order, BigDecimal settlementAmount) {
        try {
            String content = String.format("订单 %s 已办理退房，结算金额: %s", order.getOrderNumber(), settlementAmount);
            notificationService.createNotification(
                    order.getGuest().getId(),
                    order.getHomestay().getOwner().getId(),
                    NotificationType.ORDER_STATUS_CHANGED,
                    EntityType.ORDER,
                    String.valueOf(order.getId()),
                    content
            );
        } catch (Exception e) {
            log.error("发送退房通知失败: {}", e.getMessage(), e);
        }
    }

    private void sendSettlementCompletedNotification(Order order) {
        try {
            String content = String.format("订单 %s 结算完成", order.getOrderNumber());
            notificationService.createNotification(
                    order.getGuest().getId(),
                    order.getHomestay().getOwner().getId(),
                    NotificationType.ORDER_STATUS_CHANGED,
                    EntityType.ORDER,
                    String.valueOf(order.getId()),
                    content
            );
        } catch (Exception e) {
            log.error("发送结算完成通知失败: {}", e.getMessage(), e);
        }
    }
}
