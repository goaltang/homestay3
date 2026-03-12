package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    /**
     * 创建一条通知
     *
     * @param userId     接收者用户ID
     * @param actorId    触发者用户ID (可为 null)
     * @param type       通知类型
     * @param entityType 关联实体类型 (可为 null)
     * @param entityId   关联实体ID (可为 null)
     * @param content    通知内容
     * @return 创建的通知实体
     */
    Notification createNotification(Long userId, Long actorId, NotificationType type,
                                    EntityType entityType, String entityId, String content);

    /**
     * 获取指定用户的通知列表（分页）
     *
     * @param userId   用户ID
     * @param isRead   是否只看已读/未读 (null 表示所有)
     * @param type     通知类型 (null 表示所有)
     * @param pageable 分页信息
     * @return 通知 DTO 的分页结果
     */
    Page<NotificationDTO> getNotificationsForUser(Long userId, Boolean isRead, NotificationType type, Pageable pageable);

    /**
     * 获取指定用户的未读通知数量
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long getUnreadNotificationCount(Long userId);

    /**
     * 将单条通知标记为已读
     *
     * @param notificationId 通知ID
     * @param userId         当前操作的用户ID (用于权限验证)
     * @return 更新后的通知DTO
     */
    NotificationDTO markAsRead(Long notificationId, Long userId);

    /**
     * 批量将多条通知标记为已读
     *
     * @param notificationIds 通知ID列表
     * @param userId         当前操作的用户ID (用于权限验证)
     * @return 成功标记的通知数量
     */
    int markMultipleAsRead(List<Long> notificationIds, Long userId);

    /**
     * 将指定用户的所有未读通知标记为已读
     *
     * @param userId 用户ID
     * @return 标记为已读的通知数量
     */
    int markAllAsRead(Long userId);

    /**
     * 删除单条通知 (可选)
     *
     * @param notificationId 通知ID
     * @param userId         当前操作的用户ID (用于权限验证)
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * 清理过期通知 - 删除已读取且超过指定天数的通知
     *
     * @param days 保留天数（超过此天数的已读通知将被删除）
     * @return 删除的通知数量
     */
    int cleanupOldNotifications(int days);

    // --- (可选) 辅助方法 --- 
    /**
     * 发送系统公告 (给所有用户或特定角色用户 - 待实现具体逻辑)
     *
     * @param content 公告内容
     */
    // void sendSystemAnnouncement(String content);

} 