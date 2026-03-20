package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.ChatMessageDTO;
import com.homestay3.homestaybackend.dto.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket服务，用于实时推送通知
 */
@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 向特定用户推送通知
     * @param userId 用户ID
     * @param notificationDTO 通知数据传输对象
     */
    public void sendNotificationToUser(Long userId, NotificationDTO notificationDTO) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notificationDTO);
            log.info("已通过 WebSocket 推送通知给用户: {}, 消息: {}", userId, notificationDTO);
        } catch (Exception e) {
            log.error("WebSocket 推送通知失败: userId={}, error={}", userId, e.getMessage(), e);
            // 推送失败不应影响主流程
        }
    }

    /**
     * 向特定用户推送未读通知数量
     * @param userId 用户ID
     * @param count 未读通知数量
     */
    public void sendUnreadCountToUser(Long userId, long count) {
        try {
            String destination = "/topic/unread-count/" + userId;
            messagingTemplate.convertAndSend(destination, count);
            log.info("已通过 WebSocket 推送未读通知数量给用户: {}, 数量: {}", userId, count);
        } catch (Exception e) {
            log.error("WebSocket 推送未读通知数量失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 向对话房间推送聊天消息
     * @param conversationId 对话ID
     * @param chatMessageDTO 聊天消息
     */
    public void sendChatMessage(Long conversationId, ChatMessageDTO chatMessageDTO) {
        try {
            String destination = "/topic/chat/" + conversationId;
            messagingTemplate.convertAndSend(destination, chatMessageDTO);
            log.info("已通过 WebSocket 推送聊天消息到对话: {}, 内容: {}", conversationId, chatMessageDTO.getMessage().getContent());
        } catch (Exception e) {
            log.error("WebSocket 推送聊天消息失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
        }
    }

    /**
     * 向特定用户推送聊天消息（用于接收方实时更新）
     * @param userId 用户ID
     * @param chatMessageDTO 聊天消息
     */
    public void sendChatMessageToUser(Long userId, ChatMessageDTO chatMessageDTO) {
        try {
            String destination = "/topic/chat/user/" + userId;
            messagingTemplate.convertAndSend(destination, chatMessageDTO);
            log.info("已通过 WebSocket 推送聊天消息给用户: {}, 内容: {}", userId, chatMessageDTO.getMessage().getContent());
        } catch (Exception e) {
            log.error("WebSocket 推送聊天消息给用户失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}