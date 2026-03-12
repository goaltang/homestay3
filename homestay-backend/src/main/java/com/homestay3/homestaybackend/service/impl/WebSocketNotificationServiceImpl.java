package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import com.homestay3.homestaybackend.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * WebSocket通知服务实现
 * 负责通过WebSocket推送通知
 */
@Service
public class WebSocketNotificationServiceImpl implements WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationServiceImpl.class);
    private final WebSocketService webSocketService;

    public WebSocketNotificationServiceImpl(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void sendNotificationToUser(Long userId, NotificationDTO notification) {
        try {
            webSocketService.sendNotificationToUser(userId, notification);
            log.debug("已通过WebSocket推送通知给用户: {}", userId);
        } catch (Exception e) {
            log.error("WebSocket推送通知失败: userId={}, error={}", userId, e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }

    @Override
    public void sendUnreadCountToUser(Long userId, long unreadCount) {
        try {
            webSocketService.sendUnreadCountToUser(userId, unreadCount);
            log.debug("已通过WebSocket推送未读通知数量给用户: {}, 数量: {}", userId, unreadCount);
        } catch (Exception e) {
            log.error("WebSocket推送未读通知数量失败: userId={}, error={}", userId, e.getMessage(), e);
            // 不抛出异常，以免影响主业务流程
        }
    }
}