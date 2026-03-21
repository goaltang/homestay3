package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.CheckInCredentialDTO;
import com.homestay3.homestaybackend.dto.CheckInDTO;
import com.homestay3.homestaybackend.entity.CheckInRecord;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.CheckInRecordRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.CheckInService;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

/**
 * 入住服务实现
 */
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private static final Logger log = LoggerFactory.getLogger(CheckInServiceImpl.class);
    private static final String AUTO_CHECKIN_TIME_KEY = "AUTO_CHECKIN_TIME";
    private static final LocalTime DEFAULT_AUTO_CHECKIN_TIME = LocalTime.of(18, 0);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CheckInRecordRepository checkInRecordRepository;
    private final SystemConfigService systemConfigService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CheckInDTO prepareCheckIn(Long orderId, CheckInCredentialDTO credentialDTO) {
        log.info("准备入住 - 订单ID: {}", orderId);

        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：只有房东可以设置准备入住
        if (!isOrderOwner(order, currentUser)) {
            throw new AccessDeniedException("只有房东才能设置准备入住");
        }

        // 状态检查：只有 PAID 状态可以设置准备入住
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已支付状态的订单才能设置准备入住");
        }

        // 生成入住码
        String checkInCode = generateCheckInCode();

        // 解析有效时间
        LocalDateTime checkInDateTime = order.getCheckInDate().atTime(parseAutoCheckinTime());
        LocalDateTime checkOutDateTime = order.getCheckOutDate().atTime(12, 0);

        // 保存入住记录
        CheckInRecord checkInRecord = CheckInRecord.builder()
                .orderId(orderId)
                .checkInMethod(credentialDTO.getCheckInMethod() != null ? credentialDTO.getCheckInMethod() : "MANUAL")
                .checkInCode(checkInCode)
                .doorPassword(credentialDTO.getDoorPassword())
                .lockboxCode(credentialDTO.getLockboxCode())
                .locationDescription(credentialDTO.getLocationDescription())
                .validFrom(checkInDateTime)
                .validUntil(checkOutDateTime)
                .checkInOperatorId(currentUser.getId())
                .checkInOperatorType("HOST")
                .remark(credentialDTO.getRemark())
                .status("ACTIVE")
                .build();
        checkInRecordRepository.save(checkInRecord);

        // 更新订单状态
        order.setStatus(OrderStatus.READY_FOR_CHECKIN.name());
        order.setCheckInCode(checkInCode);
        order.setDoorPassword(credentialDTO.getDoorPassword());
        order.setAutoCheckinTime(parseAutoCheckinTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        orderRepository.save(order);

        // 发送通知给客人
        sendCheckInCredentialNotification(order, checkInCode, credentialDTO);

        log.info("订单 {} 已设置为准备入住状态，入住码: {}", order.getOrderNumber(), checkInCode);
        return convertToDTO(checkInRecord, order);
    }

    @Override
    public CheckInCredentialDTO getCheckInCredential(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        CheckInRecord record = checkInRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("入住记录不存在"));

        return CheckInCredentialDTO.builder()
                .checkInMethod(record.getCheckInMethod())
                .checkInCode(record.getCheckInCode())
                .doorPassword(record.getDoorPassword())
                .lockboxCode(record.getLockboxCode())
                .locationDescription(record.getLocationDescription())
                .validFrom(record.getValidFrom() != null ? record.getValidFrom().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null)
                .validUntil(record.getValidUntil() != null ? record.getValidUntil().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null)
                .remark(record.getRemark())
                .build();
    }

    @Override
    @Transactional
    public CheckInDTO performCheckIn(Long orderId) {
        log.info("办理入住 - 订单ID: {}", orderId);

        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：房东或管理员
        if (!isOrderOwner(order, currentUser) && !hasAdminAuthority(currentUser)) {
            throw new AccessDeniedException("无权办理入住");
        }

        // 状态检查：只有 READY_FOR_CHECKIN
        if (!OrderStatus.READY_FOR_CHECKIN.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不是准备入住状态");
        }

        LocalDateTime now = LocalDateTime.now();

        // 更新订单
        order.setStatus(OrderStatus.CHECKED_IN.name());
        order.setCheckedInAt(now);
        orderRepository.save(order);

        // 更新入住记录
        CheckInRecord checkInRecord = checkInRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("入住记录不存在"));
        checkInRecord.setCheckedInAt(now);
        checkInRecord.setCheckInOperatorId(currentUser.getId());
        checkInRecord.setCheckInOperatorType(hasAdminAuthority(currentUser) ? "ADMIN" : "HOST");
        checkInRecord.setCheckInMethod(checkInRecord.getCheckInMethod() != null ? checkInRecord.getCheckInMethod() : "MANUAL");
        checkInRecordRepository.save(checkInRecord);

        // 发送入住确认通知
        sendCheckInConfirmationNotification(order);

        log.info("订单 {} 已办理入住", order.getOrderNumber());
        return convertToDTO(checkInRecord, order);
    }

    @Override
    @Transactional
    public CheckInDTO selfCheckIn(String checkInCode) {
        log.info("自助入住 - 入住码: {}", checkInCode);

        CheckInRecord record = checkInRecordRepository.findByCheckInCode(checkInCode)
                .orElseThrow(() -> new IllegalArgumentException("无效的入住码"));

        if (!"ACTIVE".equals(record.getStatus())) {
            throw new IllegalArgumentException("入住码已失效");
        }

        LocalDateTime now = LocalDateTime.now();
        if (record.getValidFrom() != null && now.isBefore(record.getValidFrom())) {
            throw new IllegalArgumentException("入住码尚未生效");
        }
        if (record.getValidUntil() != null && now.isAfter(record.getValidUntil())) {
            throw new IllegalArgumentException("入住码已过期");
        }

        Order order = orderRepository.findById(record.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        if (!OrderStatus.READY_FOR_CHECKIN.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不允许入住");
        }

        // 更新订单
        order.setStatus(OrderStatus.CHECKED_IN.name());
        order.setCheckedInAt(now);
        orderRepository.save(order);

        // 更新入住记录
        record.setCheckedInAt(now);
        record.setCheckInOperatorType("SELF_SERVICE");
        record.setCheckInOperatorId(null);
        checkInRecordRepository.save(record);

        // 通知房东
        sendCheckInConfirmationNotification(order);

        log.info("订单 {} 通过自助入住码办理入住成功", order.getOrderNumber());
        return convertToDTO(record, order);
    }

    @Override
    public CheckInDTO confirmArrival(Long orderId) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：只有订单客人可以确认到达
        if (!isOrderGuest(order, currentUser)) {
            throw new AccessDeniedException("只有入住人可以确认到达");
        }

        CheckInRecord record = checkInRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("入住记录不存在"));

        // 发送通知给房东
        try {
            notificationService.createNotification(
                    order.getHomestay().getOwner().getId(),
                    currentUser.getId(),
                    NotificationType.ORDER_STATUS_CHANGED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    "客人 " + currentUser.getUsername() + " 已到达，等待办理入住"
            );
        } catch (Exception e) {
            log.error("发送到达通知失败: {}", e.getMessage(), e);
        }

        return convertToDTO(record, order);
    }

    @Override
    @Transactional
    public CheckInDTO cancelPreparation(Long orderId) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 权限检查：只有房东可以取消准备入住
        if (!isOrderOwner(order, currentUser)) {
            throw new AccessDeniedException("只有房东才能取消准备入住");
        }

        // 状态检查：只能是 READY_FOR_CHECKIN
        if (!OrderStatus.READY_FOR_CHECKIN.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不是准备入住状态");
        }

        // 更新订单状态回 PAID
        order.setStatus(OrderStatus.PAID.name());
        order.setCheckInCode(null);
        order.setDoorPassword(null);
        orderRepository.save(order);

        // 更新入住记录状态
        CheckInRecord record = checkInRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("入住记录不存在"));
        record.setStatus("CANCELLED");
        checkInRecordRepository.save(record);

        log.info("订单 {} 已取消准备入住", order.getOrderNumber());
        return convertToDTO(record, order);
    }

    @Override
    public CheckInDTO getCheckInRecord(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        CheckInRecord record = checkInRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("入住记录不存在"));

        return convertToDTO(record, order);
    }

    @Override
    public boolean validateCheckInCode(Long orderId, String checkInCode) {
        Optional<CheckInRecord> recordOpt = checkInRecordRepository.findByCheckInCode(checkInCode);
        if (recordOpt.isEmpty()) {
            return false;
        }
        CheckInRecord record = recordOpt.get();
        return record.getOrderId().equals(orderId) && "ACTIVE".equals(record.getStatus());
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

    private String generateCheckInCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    private LocalTime parseAutoCheckinTime() {
        String time = systemConfigService.getConfigValue(AUTO_CHECKIN_TIME_KEY);
        if (time != null && !time.isEmpty()) {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        }
        return DEFAULT_AUTO_CHECKIN_TIME;
    }

    private CheckInDTO convertToDTO(CheckInRecord record, Order order) {
        return CheckInDTO.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .orderNumber(order != null ? order.getOrderNumber() : null)
                .checkInMethod(record.getCheckInMethod())
                .checkedInAt(record.getCheckedInAt())
                .operatorType(record.getCheckInOperatorType())
                .checkInCode(record.getCheckInCode())
                .doorPassword(record.getDoorPassword())
                .lockboxCode(record.getLockboxCode())
                .locationDescription(record.getLocationDescription())
                .validFrom(record.getValidFrom())
                .validUntil(record.getValidUntil())
                .status(record.getStatus())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private void sendCheckInCredentialNotification(Order order, String checkInCode, CheckInCredentialDTO credentialDTO) {
        try {
            String content = String.format("您的订单 %s 已准备入住。入住码: %s%s%s%s",
                    order.getOrderNumber(),
                    checkInCode,
                    credentialDTO.getDoorPassword() != null ? "，门锁密码: " + credentialDTO.getDoorPassword() : "",
                    credentialDTO.getLockboxCode() != null ? "，密钥箱密码: " + credentialDTO.getLockboxCode() : "",
                    credentialDTO.getLocationDescription() != null ? "，位置: " + credentialDTO.getLocationDescription() : "");

            notificationService.createNotification(
                    order.getGuest().getId(),
                    order.getHomestay().getOwner().getId(),
                    NotificationType.ORDER_STATUS_CHANGED,
                    EntityType.ORDER,
                    String.valueOf(order.getId()),
                    content
            );
        } catch (Exception e) {
            log.error("发送入住凭证通知失败: {}", e.getMessage(), e);
        }
    }

    private void sendCheckInConfirmationNotification(Order order) {
        try {
            String content = String.format("订单 %s 已完成入住", order.getOrderNumber());
            notificationService.createNotification(
                    order.getGuest().getId(),
                    order.getHomestay().getOwner().getId(),
                    NotificationType.ORDER_STATUS_CHANGED,
                    EntityType.ORDER,
                    String.valueOf(order.getId()),
                    content
            );
        } catch (Exception e) {
            log.error("发送入住确认通知失败: {}", e.getMessage(), e);
        }
    }
}
