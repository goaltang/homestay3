package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationDto;
import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.*; // 导入所有 repository
import com.homestay3.homestaybackend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired; // 使用构造函数注入，无需此注解
import org.springframework.context.annotation.Lazy; // 引入 Lazy
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional; // 导入 Optional

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    // 使用构造函数注入所有依赖
    // 添加 @Lazy 防止潜在的循环依赖 (例如 A -> B -> Notification -> A)
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   @Lazy HomestayRepository homestayRepository,
                                   @Lazy OrderRepository orderRepository,
                                   @Lazy ReviewRepository reviewRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional // 确保操作的原子性
    public Notification createNotification(Long userId, Long actorId, NotificationType type, EntityType entityType, String entityId, String content) {
        Notification notification = Notification.builder()
                .userId(userId)
                .actorId(actorId)
                .type(type)
                .entityType(entityType)
                .entityId(entityId)
                .content(content)
                .isRead(false) // 默认为未读
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("创建通知成功: id={}, userId={}, type={}", savedNotification.getId(), userId, type);

        // --- 实时推送 (可选) ---
        // try {
        //     NotificationDto dto = convertToDto(savedNotification);
        //     webSocketService.sendNotificationToUser(userId, dto);
        //     log.info("已通过 WebSocket 推送通知给用户: {}", userId);
        // } catch (Exception e) {
        //     log.error("WebSocket 推送通知失败: userId={}, error={}", userId, e.getMessage(), e);
        //     // 推送失败不应影响主流程
        // }
        // --- 实时推送结束 ---

        return savedNotification;
    }

    @Override
    @Transactional(readOnly = true) // 只读事务，提高性能
    public Page<NotificationDto> getNotificationsForUser(Long userId, Boolean isRead, NotificationType type, Pageable pageable) {
        Page<Notification> notificationPage;
        
        // 根据 isRead 和 type 参数选择不同的查询方法
        if (type == null) {
            // 未指定类型
            if (isRead == null) {
                // 获取所有通知 (不区分已读未读)
                notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
                log.debug("获取用户 {} 的所有类型通知, 页码: {}, 大小: {}", userId, pageable.getPageNumber(), pageable.getPageSize());
            } else {
                // 按已读/未读状态获取所有类型通知
                notificationPage = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
                log.debug("获取用户 {} 的所有类型 {} 通知, 页码: {}, 大小: {}", userId, isRead ? "已读" : "未读", pageable.getPageNumber(), pageable.getPageSize());
            }
        } else {
            // 指定了类型
            if (isRead == null) {
                // 获取指定类型的所有通知 (不区分已读未读)
                notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
                log.debug("获取用户 {} 的类型 {} 通知, 页码: {}, 大小: {}", userId, type, pageable.getPageNumber(), pageable.getPageSize());
            } else {
                // 获取指定类型且指定已读/未读状态的通知
                notificationPage = notificationRepository.findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(userId, isRead, type, pageable);
                log.debug("获取用户 {} 的类型 {} 的 {} 通知, 页码: {}, 大小: {}", userId, type, isRead ? "已读" : "未读", pageable.getPageNumber(), pageable.getPageSize());
            }
        }

        // 将 Page<Notification> 转换为 Page<NotificationDto>
        // 注意: map 操作内部调用 convertToDto，可能涉及多次数据库查询，考虑性能优化 (如批量查询)
        return notificationPage.map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        log.debug("用户 {} 的未读通知数量: {}", userId, count);
        return count;
    }

    @Override
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知", "id", notificationId));

        // 权限验证：确保是通知的接收者才能标记为已读
        if (!notification.getUserId().equals(userId)) {
            log.warn("用户 {} 尝试标记不属于自己的通知 {}, 操作被拒绝", userId, notificationId);
            throw new AccessDeniedException("无法标记不属于您的通知");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("用户 {} 将通知 {} 标记为已读", userId, notificationId);
            return true; // 状态确实发生了改变
        } else {
            log.debug("通知 {} 已经是已读状态, 无需重复标记", notificationId);
            return false; // 状态未改变
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId);
        log.info("用户 {} 将 {} 条未读通知标记为已读", userId, updatedCount);

        // --- 实时推送 (可选) ---
        // 如果标记了任何通知为已读，可以推送更新后的未读计数
        // if (updatedCount > 0) {
        //     try {
        //         long newUnreadCount = getUnreadNotificationCount(userId);
        //         webSocketService.sendUnreadCountToUser(userId, newUnreadCount);
        //     } catch (Exception e) {
        //         log.error("WebSocket 推送未读计数失败: userId={}, error={}", userId, e.getMessage(), e);
        //     }
        // }
        // --- 实时推送结束 ---

        return updatedCount;
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知", "id", notificationId));

        // 权限验证：确保是通知的接收者才能删除
        if (!notification.getUserId().equals(userId)) {
            log.warn("用户 {} 尝试删除不属于自己的通知 {}, 操作被拒绝", userId, notificationId);
            throw new AccessDeniedException("无法删除不属于您的通知");
        }

        notificationRepository.delete(notification);
        log.info("用户 {} 删除了通知 {}", userId, notificationId);
    }

    // --- 私有辅助方法 ---
    private NotificationDto convertToDto(Notification notification) {
        // log.info("[NotificationService] Converting notification ID: {}, isRead from entity: {}", notification.getId(), notification.isRead()); 
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setActorId(notification.getActorId());
        dto.setType(notification.getType());
        dto.setEntityType(notification.getEntityType());
        dto.setEntityId(notification.getEntityId());
        dto.setContent(notification.getContent()); // 保留原始内容作为后备
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());

        // 填充 actorUsername
        if (notification.getActorId() != null) {
            userRepository.findById(notification.getActorId())
                    .ifPresent(actor -> dto.setActorUsername(actor.getUsername()));
        }

        // 填充 entityTitle (根据 entityType 和 entityId)
        if (notification.getEntityType() != null && notification.getEntityId() != null) {
            try {
                switch (notification.getEntityType()) {
                    case ORDER:
                    case BOOKING: // BOOKING 和 ORDER 通常关联订单
                        Long orderId = Long.parseLong(notification.getEntityId());
                        orderRepository.findById(orderId)
                                .ifPresent(order -> dto.setEntityTitle("订单 " + order.getOrderNumber()));
                        break;
                    case HOMESTAY:
                        Long homestayId = Long.parseLong(notification.getEntityId());
                        homestayRepository.findById(homestayId)
                                .ifPresent(homestay -> dto.setEntityTitle(homestay.getTitle()));
                        break;
                    case REVIEW:
                        Long reviewId = Long.parseLong(notification.getEntityId());
                        // 对于评价，entityTitle 可以是关联的房源名称
                        reviewRepository.findHomestayTitleByReviewId(reviewId)
                                .ifPresent(dto::setEntityTitle);
                        break;
                    case USER:
                        // 如果实体是用户（例如关注通知），entityTitle 可以是用户名
                        Long userId = Long.parseLong(notification.getEntityId());
                        userRepository.findById(userId)
                                .ifPresent(user -> dto.setEntityTitle(user.getUsername()));
                        break;
                    // case SYSTEM:
                    //     // 系统通知通常没有特定实体标题
                    //     break;
                    default:
                        log.warn("Unhandled entity type for title population: {}", notification.getEntityType());
                }
            } catch (NumberFormatException e) {
                log.error("Failed to parse entityId '{}' for entityType {} while converting notification to DTO",
                        notification.getEntityId(), notification.getEntityType(), e);
            } catch (Exception e) {
                log.error("Error fetching entity details for notification DTO (id={}): {}", notification.getId(), e.getMessage(), e);
            }
        }

        return dto;
    }
} 