package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 根据用户ID分页查找通知，按创建时间降序排序
     * @param userId 用户ID
     * @param pageable 分页信息
     * @return 通知分页结果
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID和已读状态分页查找通知，按创建时间降序排序
     * @param userId 用户ID
     * @param isRead 是否已读
     * @param pageable 分页信息
     * @return 通知分页结果
     */
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, boolean isRead, Pageable pageable);

    /**
     * 根据用户ID和类型分页查找通知，按创建时间降序排序
     * @param userId 用户ID
     * @param type 通知类型
     * @param pageable 分页信息
     * @return 通知分页结果
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    /**
     * 根据用户ID、已读状态和类型分页查找通知，按创建时间降序排序
     * @param userId 用户ID
     * @param isRead 是否已读
     * @param type 通知类型
     * @param pageable 分页信息
     * @return 通知分页结果
     */
    Page<Notification> findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(Long userId, boolean isRead, NotificationType type, Pageable pageable);

    /**
     * 统计指定用户的未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 将指定用户的所有未读通知标记为已读
     * @param userId 用户ID
     * @return 更新的记录数
     */
    @Modifying // 表示这是一个更新或删除操作
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 查找指定用户的所有未读通知ID
     * @param userId 用户ID
     * @return 未读通知ID列表
     */
    @Query("SELECT n.id FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    List<Long> findUnreadNotificationIdsByUserId(@Param("userId") Long userId);

} 