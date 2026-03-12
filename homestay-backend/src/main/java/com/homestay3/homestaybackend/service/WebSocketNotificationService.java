package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.NotificationDTO;

/**
 * 处理通过WebSocket推送通知的服务
 * 将此功能从NotificationService中解耦出来
 */
public interface WebSocketNotificationService {

    /**
     * 通过WebSocket发送通知给指定用户
     * @param userId 用户ID
     * @param notification 要发送的通知
     */
    void sendNotificationToUser(Long userId, NotificationDTO notification);

    /**
     * 通过WebSocket发送未读通知数量给指定用户
     * @param userId 用户ID
     * @param unreadCount 未读通知数量
     */
    void sendUnreadCountToUser(Long userId, long unreadCount);
}