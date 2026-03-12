package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;

/**
 * 订单通知服务
 * 封装所有与订单相关的通知发送逻辑
 */
public interface OrderNotificationService {

    /**
     * 发送订单确认通知（房源自动确认时）
     * @param hostId 房东用户ID
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param hostUsername 房东用户名
     * @param guestUsername 客人用户名
     * @param orderNumber 订单号
     */
    void sendOrderAutoConfirmedNotification(Long hostId, Long guestId, Long orderId,
                                           String hostUsername, String guestUsername, String orderNumber);

    /**
     * 发送订单预订请求通知（房东确认制时）
     * @param hostId 房东用户ID
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param guestUsername 客人用户名
     * @param orderNumber 订单号
     */
    void sendOrderBookingRequestNotification(Long hostId, Long guestId, Long orderId,
                                            String guestUsername, String orderNumber);

    /**
     * 发送订单确认通知（房东手动确认时）
     * @param guestId 客人用户ID
     * @param hostId 房东用户ID (触发者)
     * @param orderId 订单ID
     * @param guestUsername 客人用户名
     * @param homestayTitle 房源标题
     * @param hostUsername 房东用户名
     * @param orderNumber 订单号
     */
    void sendOrderConfirmedNotification(Long guestId, Long hostId, Long orderId,
                                       String guestUsername, String homestayTitle,
                                       String hostUsername, String orderNumber);

    /**
     * 发送订单完成通知
     * @param guestId 客人用户ID
     * @param hostId 房东用户ID
     * @param orderId 订单ID
     * @param guestUsername 客人用户名
     * @param hostUsername 房东用户名
     * @param homestayTitle 房源标题
     * @param orderNumber 订单号
     * @param triggerUserId 触发操作的用户ID（可为null）
     */
    void sendOrderCompletedNotification(Long guestId, Long hostId, Long orderId,
                                       String guestUsername, String hostUsername,
                                       String homestayTitle, String orderNumber,
                                       Long triggerUserId);

    /**
     * 发送订单支付成功通知
     * @param hostId 房东用户ID
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param hostUsername 房东用户名
     * @param guestUsername 客人用户名
     * @param homestayTitle 房源标题
     * @param orderNumber 订单号
     * @param paymentMethod 支付方式
     */
    void sendOrderPaymentSuccessNotification(Long hostId, Long guestId, Long orderId,
                                            String hostUsername, String guestUsername,
                                            String homestayTitle, String orderNumber,
                                            String paymentMethod);

    /**
     * 发送订单被拒绝通知
     * @param guestId 客人用户ID
     * @param hostId 房东用户ID (触发者)
     * @param orderId 订单ID
     * @param guestUsername 客人用户名
     * @param homestayTitle 房源标题
     * @param orderNumber 订单号
     * @param rejectReason 拒绝原因
     */
    void sendOrderRejectedNotification(Long guestId, Long hostId, Long orderId,
                                      String guestUsername, String homestayTitle,
                                      String orderNumber, String rejectReason);

    /**
     * 发送订单取消通知
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param homestayTitle 房源标题
     * @param previousStatus 之前的状态
     * @param newStatus 新的状态
     * @param cancelReason 取消原因
     * @param cancelledByUser 是否由用户触发取消（否则由房东或系统触发）
     */
    void sendOrderCancelledNotification(Long guestId, Long orderId, String orderNumber,
                                       String homestayTitle, String previousStatus,
                                       String newStatus, String cancelReason,
                                       boolean cancelledByUser);

    /**
     * 发送订单退款申请通知
     * @param guestId 客人用户ID (发起退款的用户)
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param refundReason 退款原因
     * @param refundType 退款类型
     * @param refundAmount 退款金额
     */
    void sendOrderRefundRequestedNotification(Long guestId, Long orderId, String orderNumber,
                                             String refundReason, String refundType,
                                             String refundAmount);

    /**
     * 发送订单退款批准通知
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param refundNote 批准备注
     */
    void sendOrderRefundApprovedNotification(Long guestId, Long orderId, String orderNumber,
                                            String refundNote);

    /**
     * 发送订单退款拒绝通知
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param rejectReason 拒绝原因
     */
    void sendOrderRefundRejectedNotification(Long guestId, Long orderId, String orderNumber,
                                            String rejectReason);

    /**
     * 发送订单退款完成通知
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param transactionId 退款交易号
     */
    void sendOrderRefundCompletedNotification(Long guestId, Long orderId, String orderNumber,
                                             String transactionId);

    /**
     * 发送订单退款启动通知（当退款申请被提交时）
     * @param guestId 客人用户ID
     * @param orderId 订单ID
     * @param orderNumber 订单号
     * @param initiatorId 发起退款的用户ID（用户、房东或管理员）
     */
    void sendOrderRefundInitiatedNotification(Long guestId, Long orderId, String orderNumber,
                                             Long initiatorId);
}