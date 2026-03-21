package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.OrderNotificationService;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 订单通知服务实现
 * 封装所有与订单相关的通知发送逻辑
 */
@Service
public class OrderNotificationServiceImpl implements OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationServiceImpl.class);
    private final NotificationService notificationService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final UserRepository userRepository;

    public OrderNotificationServiceImpl(NotificationService notificationService,
                                        WebSocketNotificationService webSocketNotificationService,
                                        UserRepository userRepository) {
        this.notificationService = notificationService;
        this.webSocketNotificationService = webSocketNotificationService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void sendOrderAutoConfirmedNotification(Long hostId, Long guestId, Long orderId,
                                                   String hostUsername, String guestUsername, String orderNumber) {
        try {
            // 通知房东
            String hostNotificationContent = String.format(
                    "您收到了来自用户 %s 的新订单 (订单 %s)，该订单已自动确认，等待用户支付。",
                    guestUsername, orderNumber);
            notificationService.createNotification(
                    hostId, guestId,
                    NotificationType.ORDER_CONFIRMED, EntityType.BOOKING,
                    String.valueOf(orderId), hostNotificationContent);

            // 通知客人
            String guestNotificationContent = String.format(
                    "您的预订 (订单 %s) 已自动确认，请在2小时内完成支付。",
                    orderNumber);
            notificationService.createNotification(
                    guestId, hostId,
                    NotificationType.ORDER_CONFIRMED, EntityType.BOOKING,
                    String.valueOf(orderId), guestNotificationContent);

            // 实时推送
            sendNotificationsToUsers(hostId, guestId);

            log.info("已发送自动确认订单通知 - 房东: {}, 客人: {}, 订单: {}",
                    hostUsername, guestUsername, orderNumber);
        } catch (Exception e) {
            log.error("发送自动确认订单通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderBookingRequestNotification(Long hostId, Long guestId, Long orderId,
                                                    String guestUsername, String orderNumber) {
        try {
            // 通知房东
            String notificationContent = String.format(
                    "您收到了来自用户 %s 的新预订请求 (订单 %s)，请及时处理。",
                    guestUsername, orderNumber);
            notificationService.createNotification(
                    hostId, guestId,
                    NotificationType.BOOKING_REQUEST, EntityType.BOOKING,
                    String.valueOf(orderId), notificationContent);

            // 实时推送
            webSocketNotificationService.sendNotificationToUser(hostId, 
                    createBasicNotificationDTO(guestId, hostId, NotificationType.BOOKING_REQUEST, 
                            EntityType.BOOKING, String.valueOf(orderId), notificationContent));

            log.info("已为房东 {} 发送新预订请求通知 (订单 {})",
                    guestUsername, orderNumber);
        } catch (Exception e) {
            log.error("发送新预订请求通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderConfirmedNotification(Long guestId, Long hostId, Long orderId,
                                               String guestUsername, String homestayTitle,
                                               String hostUsername, String orderNumber) {
        try {
            // 通知客人
            String notificationContent = String.format(
                    "您的订单 %s (房源: %s) 已被房东 %s 确认，请及时支付。",
                    orderNumber, homestayTitle, hostUsername);
            notificationService.createNotification(
                    guestId, hostId,
                    NotificationType.ORDER_CONFIRMED, EntityType.ORDER,
                    String.valueOf(orderId), notificationContent);

            // 实时推送
            webSocketNotificationService.sendNotificationToUser(guestId, 
                    createBasicNotificationDTO(hostId, guestId, NotificationType.ORDER_CONFIRMED, 
                            EntityType.ORDER, String.valueOf(orderId), notificationContent));

            log.info("已为房客 {} 发送订单 {} 确认通知", guestUsername, orderNumber);
        } catch (Exception e) {
            log.error("发送订单确认通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderCompletedNotification(Long guestId, Long hostId, Long orderId,
                                               String guestUsername, String hostUsername,
                                               String homestayTitle, String orderNumber,
                                               Long triggerUserId) {
        try {
            User guest = userRepository.findById(guestId).orElse(null);
            User host = userRepository.findById(hostId).orElse(null);
            if (guest == null || host == null) {
                log.warn("无法发送订单完成通知，找不到用户 - guestId: {}, hostId: {}", guestId, hostId);
                return;
            }

            String homestayTitleSafe = Objects.requireNonNullElse(homestayTitle, "未知房源");
            String orderNumberSafe = Objects.requireNonNullElse(orderNumber, "未知订单");
            String contentBase = String.format("订单 %s (房源: %s) 已完成", orderNumberSafe, homestayTitleSafe);
            String contentForGuest = contentBase + "。感谢您的入住！期待您再次光临。";
            String contentForHost = contentBase + "。请尽快结算相关收益。";

            // 通知房客
            notificationService.createNotification(
                    guest.getId(), triggerUserId, NotificationType.ORDER_COMPLETED, EntityType.ORDER,
                    String.valueOf(orderId), contentForGuest);
            log.info("已为房客 {} 发送订单 {} 完成通知", guest.getUsername(), orderNumberSafe);

            // 通知房东
            notificationService.createNotification(
                    host.getId(), triggerUserId, NotificationType.ORDER_COMPLETED, EntityType.ORDER,
                    String.valueOf(orderId), contentForHost);
            log.info("已为房东 {} 发送订单 {} 完成通知", host.getUsername(), orderNumberSafe);

            // 实时推送未读数量更新
            if (triggerUserId != null) {
                Long[] userIds = {guestId, hostId};
                for (Long userId : userIds) {
                    try {
                        long newUnreadCount = getUnreadNotificationCount(userId);
                        webSocketNotificationService.sendUnreadCountToUser(userId, newUnreadCount);
                    } catch (Exception ex) {
                        log.error("WebSocket推送未读计数失败: userId={}, error={}", userId, ex.getMessage(), ex);
                    }
                }
            }

        } catch (Exception e) {
            log.error("发送订单完成通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderPaymentSuccessNotification(Long hostId, Long guestId, Long orderId,
                                                    String hostUsername, String guestUsername,
                                                    String homestayTitle, String orderNumber,
                                                    String paymentMethod) {
        try {
            // 通知房东
            String notificationContentForHost = String.format(
                    "用户 %s 已支付订单 %s (房源: %s)。",
                    guestUsername, orderNumber, homestayTitle);
            notificationService.createNotification(
                    hostId, guestId,
                    NotificationType.PAYMENT_RECEIVED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContentForHost);

            // 通知客人
            String notificationContentForGuest = String.format(
                    "您的订单 %s (房源: %s) 已被房东确认并支付成功！",
                    orderNumber, homestayTitle);
            notificationService.createNotification(
                    guestId, hostId,
                    NotificationType.BOOKING_ACCEPTED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContentForGuest);

            // 实时推送
            webSocketNotificationService.sendNotificationToUser(hostId, 
                    createBasicNotificationDTO(guestId, hostId, NotificationType.PAYMENT_RECEIVED, 
                            EntityType.ORDER, String.valueOf(orderId), notificationContentForHost));
            webSocketNotificationService.sendNotificationToUser(guestId, 
                    createBasicNotificationDTO(hostId, guestId, NotificationType.BOOKING_ACCEPTED, 
                            EntityType.ORDER, String.valueOf(orderId), notificationContentForGuest));

            log.info("已为房东 {} 发送订单 {} 支付成功通知", hostUsername, orderNumber);
            log.info("已为房客 {} 发送订单 {} 被接受的通知", guestUsername, orderNumber);
        } catch (Exception e) {
            log.error("发送订单支付成功通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderRejectedNotification(Long guestId, Long hostId, Long orderId,
                                              String guestUsername, String homestayTitle,
                                              String orderNumber, String rejectReason) {
        try {
            String notificationContent = String.format(
                    "很抱歉，您关于房源 '%s' 的预订请求 (订单 %s) 已被房东拒绝。原因: %s",
                    homestayTitle, orderNumber,
                    (rejectReason != null && !rejectReason.isEmpty()) ? rejectReason : "未提供具体原因");
            notificationService.createNotification(
                    guestId, hostId,
                    NotificationType.BOOKING_REJECTED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContent);
            log.info("已为房客 {} 发送订单 {} 被拒绝的通知", guestUsername, orderNumber);
        } catch (Exception e) {
            log.error("发送订单被拒绝通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderCancelledNotification(Long guestId, Long orderId, String orderNumber,
                                               String homestayTitle, String previousStatus,
                                               String newStatus, String cancelReason,
                                               boolean cancelledByUser) {
        try {
            User guest = userRepository.findById(guestId).orElse(null);
            if (guest == null) {
                log.warn("无法发送订单取消通知，找不到客人用户: {}", guestId);
                return;
            }

            String notificationContent;
            if (OrderStatus.REFUND_PENDING.name().equals(newStatus)) {
                // 退款通知
                notificationContent = String.format(
                        "您的订单 %s (房源: %s) 退款申请已提交，我们将在1-3个工作日内处理。原因: %s",
                        orderNumber,
                        homestayTitle,
                        cancelReason != null && !cancelReason.isEmpty() ? cancelReason : "未提供原因");
                notificationService.createNotification(
                        guest.getId(), cancelledByUser ? guestId : null,
                        NotificationType.REFUND_REQUESTED,
                        EntityType.ORDER,
                        String.valueOf(orderId),
                        notificationContent);
            } else {
                // 取消通知
                notificationContent = String.format(
                        "您的订单 %s (房源: %s) 已被取消。状态从 %s 变为 %s。原因: %s",
                        orderNumber,
                        homestayTitle,
                        previousStatus,
                        newStatus,
                        cancelReason != null && !cancelReason.isEmpty() ? cancelReason : "未提供原因");
                notificationService.createNotification(
                        guest.getId(), cancelledByUser ? guestId : null,
                        NotificationType.ORDER_STATUS_CHANGED,
                        EntityType.ORDER,
                        String.valueOf(orderId),
                        notificationContent);
            }

            log.info("已为用户 {} 发送订单 {} 处理通知", guest.getUsername(), orderNumber);
        } catch (Exception e) {
            log.error("发送订单取消通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderRefundRequestedNotification(Long guestId, Long orderId, String orderNumber,
                                                     String refundReason, String refundType,
                                                     String refundAmount) {
        try {
            String refundNote = String.format("退款申请 - 类型: %s, 原因: %s, 申请人: %s, 计算退款金额: %s",
                    refundType,
                    refundReason != null && !refundReason.isEmpty() ? refundReason : "用户申请退款",
                    getCurrentUsername(guestId),
                    refundAmount);
            
            // 这里我们只记录日志，实际的退款申请通知应该在发起退款时由OrderService处理
            // 或者我们可以创建一个专门的通知，但为了保持与现有逻辑的一致性，这里先记录日志
            log.info("用户 {} 退款申请已提交，订单号: {}, 退款原因: {}", 
                    getCurrentUsername(guestId), orderNumber, refundReason);
        } catch (Exception e) {
            log.error("处理订单退款申请通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderRefundApprovedNotification(Long guestId, Long orderId, String orderNumber,
                                                    String refundNote) {
        try {
            // 通知客人
            String notificationContent = String.format(
                    "好消息！您的订单 %s 退款申请已批准并完成，款项已原路退回，请注意查收。",
                    orderNumber);
            notificationService.createNotification(
                    guestId, getCurrentUserId(), // 触发者为当前用户（管理员/房东）
                    NotificationType.REFUND_COMPLETED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContent);
            log.info("已为房客 {} 发送订单 {} 退款完成通知", getCurrentUsername(guestId), orderNumber);
        } catch (Exception e) {
            log.error("发送订单退款批准通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderRefundRejectedNotification(Long guestId, Long orderId, String orderNumber,
                                                    String rejectReason) {
        try {
            // 通知客人
            String notificationContent = String.format(
                    "很抱歉，您的订单 %s 退款申请被拒绝。原因: %s。如有疑问请联系客服。",
                    orderNumber,
                    rejectReason != null && !rejectReason.isEmpty() ? rejectReason : "未提供原因");
            notificationService.createNotification(
                    guestId, getCurrentUserId(), // 触发者为当前用户（管理员/房东）
                    NotificationType.REFUND_REJECTED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContent);
            log.info("已为房客 {} 发送订单 {} 退款拒绝通知", getCurrentUsername(guestId), orderNumber);
        } catch (Exception e) {
            log.error("发送订单退款拒绝通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderRefundCompletedNotification(Long guestId, Long orderId, String orderNumber,
                                                     String transactionId) {
        try {
            // 通知客人
            String notificationContent = String.format(
                    "好消息！您的订单 %s 退款已完成，款项已原路退回，请注意查收。退款交易号: %s",
                    orderNumber,
                    transactionId != null && !transactionId.isEmpty() ? transactionId : "请查看银行流水");
            notificationService.createNotification(
                    guestId, getCurrentUserId(), // 触发者为当前用户（管理员/房东）
                    NotificationType.REFUND_COMPLETED,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContent);
            log.info("已为房客 {} 发送订单 {} 退款完成通知", getCurrentUsername(guestId), orderNumber);
        } catch (Exception e) {
            log.error("发送订单退款完成通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendOrderRefundInitiatedNotification(Long guestId, Long orderId, String orderNumber,
                                                     Long initiatorId) {
        try {
            String initiatorUsername = getCurrentUsername(initiatorId);
            String notificationContent = String.format(
                    "您的订单 %s 退款申请已提交，我们将在1-3个工作日内处理。发起人: %s",
                    orderNumber,
                    initiatorUsername != null ? initiatorUsername : "系统");
            
            // 确定通知类型
            NotificationType notificationType;
            if (initiatorId != null && initiatorId.equals(guestId)) {
                // 用户自己发起的退款
                notificationType = NotificationType.REFUND_REQUESTED;
            } else {
                // 房东或管理员发起的退款
                notificationType = NotificationType.REFUND_REQUESTED; // 或者可以使用其他类型
            }
            
            notificationService.createNotification(
                    guestId, initiatorId,
                    notificationType,
                    EntityType.ORDER,
                    String.valueOf(orderId),
                    notificationContent);
            
            log.info("已为房客 {} 发送订单 {} 退款申请通知，发起人: {}",
                    getCurrentUsername(guestId), orderNumber, initiatorUsername);
        } catch (Exception e) {
            log.error("发送订单退款申请通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendDisputeRaisedNotification(Long orderId, Long guestId, Long hostId,
                                            String orderNumber, String homestayTitle,
                                            String disputeReason) {
        try {
            // 通知房东
            if (hostId != null) {
                String hostNotificationContent = String.format(
                        "订单 %s (房源: %s) 有新的争议待处理。争议原因: %s，请等待管理员仲裁。",
                        orderNumber, homestayTitle, disputeReason != null ? disputeReason : "未提供");
                notificationService.createNotification(
                        hostId, guestId,
                        NotificationType.REFUND_REQUESTED,
                        EntityType.ORDER,
                        String.valueOf(orderId),
                        hostNotificationContent);
                log.info("已为房东 {} 发送订单 {} 争议发起通知", getCurrentUsername(hostId), orderNumber);
            }

            // 通知管理员（如果有管理员角色用户，这里简化处理，实际可能需要查询所有管理员）
            // 通知客人
            if (guestId != null) {
                String guestNotificationContent = String.format(
                        "您关于订单 %s (房源: %s) 的退款申请已进入争议处理流程，管理员将进行仲裁，请等待结果。",
                        orderNumber, homestayTitle);
                notificationService.createNotification(
                        guestId, hostId,
                        NotificationType.REFUND_REQUESTED,
                        EntityType.ORDER,
                        String.valueOf(orderId),
                        guestNotificationContent);
                log.info("已为房客 {} 发送订单 {} 争议发起通知", getCurrentUsername(guestId), orderNumber);
            }
        } catch (Exception e) {
            log.error("发送争议发起通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    @Transactional
    public void sendDisputeResolvedNotification(Long orderId, Long guestId, Long hostId,
                                             String orderNumber, String homestayTitle,
                                             String resolution, String note) {
        try {
            String resolutionText = "APPROVED".equals(resolution) ? "已批准退款" : "已拒绝退款";
            String guestContent;
            String hostContent;

            if ("APPROVED".equals(resolution)) {
                guestContent = String.format(
                        "好消息！您关于订单 %s (房源: %s) 的退款申请已被管理员仲裁%s，款项将原路退回。",
                        orderNumber, homestayTitle,
                        note != null && !note.isEmpty() ? "，备注: " + note : "");
                hostContent = String.format(
                        "管理员已对订单 %s (房源: %s) 的争议作出仲裁，%s。",
                        orderNumber, homestayTitle, resolutionText);
            } else {
                guestContent = String.format(
                        "很抱歉，您关于订单 %s (房源: %s) 的退款申请已被管理员仲裁拒绝，订单已恢复为已支付状态。如有疑问请联系客服。",
                        orderNumber, homestayTitle);
                hostContent = String.format(
                        "管理员已对订单 %s (房源: %s) 的争议作出仲裁，%s。订单已恢复为已支付状态。",
                        orderNumber, homestayTitle, resolutionText);
            }

            // 通知房东
            if (hostId != null) {
                notificationService.createNotification(
                        hostId, guestId,
                        NotificationType.ORDER_STATUS_CHANGED,
                        EntityType.ORDER,
                        String.valueOf(orderId),
                        hostContent);
                log.info("已为房东 {} 发送订单 {} 争议解决通知", getCurrentUsername(hostId), orderNumber);
            }

            // 通知客人
            if (guestId != null) {
                notificationService.createNotification(
                        guestId, hostId,
                        NotificationType.REFUND_COMPLETED,
                        EntityType.ORDER,
                        String.valueOf(orderId),
                        guestContent);
                log.info("已为房客 {} 发送订单 {} 争议解决通知", getCurrentUsername(guestId), orderNumber);
            }
        } catch (Exception e) {
            log.error("发送争议解决通知失败: {}", e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    // 辅助方法：获取当前用户ID
    private Long getCurrentUserId() {
        try {
            return userRepository.findByUsername(
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName())
                    .map(User::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("无法获取当前用户ID: {}", e.getMessage());
            return null;
        }
    }

    // 辅助方法：获取当前用户名
    private String getCurrentUsername(Long userId) {
        try {
            return userRepository.findById(userId)
                    .map(User::getUsername)
                    .orElse("未知用户");
        } catch (Exception e) {
            log.warn("无法获取用户名: {}", e.getMessage());
            return "未知用户";
        }
    }

    // 辅助方法：发送通知给多个用户（用于自动确认场景）
    private void sendNotificationsToUsers(Long... userIds) {
        for (Long userId : userIds) {
            if (userId != null) {
                try {
                    webSocketNotificationService.sendUnreadCountToUser(userId, 
                            getUnreadNotificationCount(userId));
                } catch (Exception e) {
                    log.error("WebSocket推送未读计数失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
        }
    }

    // 辅助方法：获取用户未读通知数量
    private long getUnreadNotificationCount(Long userId) {
        // 这里需要通过notificationService获取未读数量
        // 但NotificationService没有直接提供这个方法，我们需要变通
        // 实际上，NotificationServiceImpl有getUnreadNotificationCount方法
        // 由于我们只能依赖NotificationService接口，这里需要另一种方法
        
        // 临时解决方案：我们可以直接使用notificationService的实现
        // 但这样会耦合到实现类，不是理想的做法
        // 更好的做法是扩展NotificationService接口
        
        // 为了不修改现有接口，这里我们尝试强制转换（在实际项目中应该改进接口设计）
        if (notificationService instanceof com.homestay3.homestaybackend.service.impl.NotificationServiceImpl) {
            return ((com.homestay3.homestaybackend.service.impl.NotificationServiceImpl) notificationService)
                    .getUnreadNotificationCount(userId);
        }
        // 如果不是我们期望的实现，返回0以避免错误
        return 0;
    }

    // 辅助方法：创建基础通知DTO
    private NotificationDTO createBasicNotificationDTO(Long actorId, Long userId, NotificationType type,
                                                       EntityType entityType, String entityId, String content) {
        NotificationDTO dto = new NotificationDTO();
        dto.setActorId(actorId);
        dto.setUserId(userId);
        dto.setType(type);
        dto.setEntityType(entityType);
        dto.setEntityId(entityId);
        dto.setContent(content);
        dto.setRead(false);
        return dto;
    }
}