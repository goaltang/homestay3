package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.entity.Notification;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.Review;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.*; // 导入所有 repository
import com.homestay3.homestaybackend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired; // 使用构造函数注入，无需此注解
import org.springframework.context.annotation.Lazy; // 引入 Lazy
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional; // 导入 Optional
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Page<NotificationDTO> getNotificationsForUser(Long userId, Boolean isRead, NotificationType type, Pageable pageable) {
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

        // 将 Page<Notification> 转换为 Page<NotificationDTO>
        // 使用批量转换优化性能，避免N+1查询问题
        List<NotificationDTO> dtoList = convertToDtosBatch(notificationPage.getContent());
        return new PageImpl<>(dtoList, pageable, notificationPage.getTotalElements());
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
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知", "id", notificationId));

        if (!notification.getUserId().equals(userId)) {
            log.warn("用户 {} 尝试标记不属于自己的通知 {}, 操作被拒绝", userId, notificationId);
            throw new AccessDeniedException("无法标记不属于您的通知");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            log.info("用户 {} 将通知 {} 标记为已读", userId, notificationId);
        } else {
            log.debug("通知 {} 已经是已读状态", notificationId);
        }

        return convertToDtoWithFullData(notification);
    }

    @Override
    @Transactional
    public int markMultipleAsRead(List<Long> notificationIds, Long userId) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }

        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Notification notification : notifications) {
            if (notification.getUserId().equals(userId) && !notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(now);
                count++;
            }
        }

        notificationRepository.saveAll(notifications);
        log.info("用户 {} 批量标记 {} 条通知为已读", userId, count);
        return count;
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

    @Override
    @Transactional
    public int cleanupOldNotifications(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        int deletedCount = notificationRepository.deleteReadNotificationsOlderThan(cutoffDate);
        log.info("清理 {} 天前的已读通知，删除了 {} 条", days, deletedCount);
        return deletedCount;
    }

    // --- 私有辅助方法 ---

    /**
     * 批量转换通知为DTO，使用批量查询优化性能
     */
    private List<NotificationDTO> convertToDtosBatch(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有需要查询的ID
        Set<Long> actorIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        Set<Long> homestayIds = new HashSet<>();
        Set<Long> reviewIds = new HashSet<>();
        Set<Long> userIdsForEntity = new HashSet<>();

        for (Notification notification : notifications) {
            // 收集actorId
            if (notification.getActorId() != null) {
                actorIds.add(notification.getActorId());
            }

            // 收集entityId根据类型
            if (notification.getEntityType() != null && notification.getEntityId() != null) {
                try {
                    Long entityId = Long.parseLong(notification.getEntityId());
                    switch (notification.getEntityType()) {
                        case ORDER:
                        case BOOKING:
                            orderIds.add(entityId);
                            break;
                        case HOMESTAY:
                            homestayIds.add(entityId);
                            break;
                        case REVIEW:
                            reviewIds.add(entityId);
                            break;
                        case USER:
                            userIdsForEntity.add(entityId);
                            break;
                        default:
                            // 忽略其他类型
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析entityId: {} 对于通知ID: {}", notification.getEntityId(), notification.getId());
                }
            }
        }

        // 批量查询
        Map<Long, User> actorMap = actorIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, Order> orderMap = orderIds.isEmpty() ? Collections.emptyMap() :
                orderRepository.findAllById(orderIds).stream()
                        .collect(Collectors.toMap(Order::getId, Function.identity()));

        Map<Long, Homestay> homestayMap = homestayIds.isEmpty() ? Collections.emptyMap() :
                homestayRepository.findAllById(homestayIds).stream()
                        .collect(Collectors.toMap(Homestay::getId, Function.identity()));

        Map<Long, String> reviewHomestayTitleMap = new HashMap<>();
        if (!reviewIds.isEmpty()) {
            List<Object[]> reviewResults = reviewRepository.findHomestayTitlesByReviewIds(new ArrayList<>(reviewIds));
            for (Object[] result : reviewResults) {
                Long reviewId = (Long) result[0];
                String title = (String) result[1];
                reviewHomestayTitleMap.put(reviewId, title);
            }
        }

        Map<Long, User> userEntityMap = userIdsForEntity.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(userIdsForEntity).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        // 转换每个通知
        List<NotificationDTO> dtos = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            dtos.add(convertToDtoWithMaps(notification, actorMap, orderMap, homestayMap, reviewHomestayTitleMap, userEntityMap));
        }

        return dtos;
    }

    /**
     * 使用预加载的映射数据转换单个通知
     */
    private NotificationDTO convertToDtoWithMaps(
            Notification notification,
            Map<Long, User> actorMap,
            Map<Long, Order> orderMap,
            Map<Long, Homestay> homestayMap,
            Map<Long, String> reviewHomestayTitleMap,
            Map<Long, User> userEntityMap) {

        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setActorId(notification.getActorId());
        dto.setType(notification.getType());
        dto.setEntityType(notification.getEntityType());
        dto.setEntityId(notification.getEntityId());
        dto.setContent(notification.getContent());
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());

        // 填充 actorUsername
        if (notification.getActorId() != null) {
            User actor = actorMap.get(notification.getActorId());
            if (actor != null) {
                dto.setActorUsername(actor.getUsername());
            }
        }

        // 填充 entityTitle
        if (notification.getEntityType() != null && notification.getEntityId() != null) {
            try {
                Long entityId = Long.parseLong(notification.getEntityId());
                switch (notification.getEntityType()) {
                    case ORDER:
                    case BOOKING:
                        Order order = orderMap.get(entityId);
                        if (order != null) {
                            dto.setEntityTitle("订单 " + order.getOrderNumber());
                        }
                        break;
                    case HOMESTAY:
                        Homestay homestay = homestayMap.get(entityId);
                        if (homestay != null) {
                            dto.setEntityTitle(homestay.getTitle());
                        }
                        break;
                    case REVIEW:
                        String title = reviewHomestayTitleMap.get(entityId);
                        if (title != null) {
                            dto.setEntityTitle(title);
                        }
                        break;
                    case USER:
                        User user = userEntityMap.get(entityId);
                        if (user != null) {
                            dto.setEntityTitle(user.getUsername());
                        }
                        break;
                    default:
                        log.debug("未处理的实体类型: {}", notification.getEntityType());
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析entityId '{}' 对于通知ID {}: {}",
                        notification.getEntityId(), notification.getId(), e.getMessage());
            }
        }

        return dto;
    }

    /**
     * 转换单个通知为DTO，包含完整的关联数据
     */
    private NotificationDTO convertToDtoWithFullData(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setActorId(notification.getActorId());
        dto.setType(notification.getType());
        dto.setEntityType(notification.getEntityType());
        dto.setEntityId(notification.getEntityId());
        dto.setContent(notification.getContent());
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());

        if (notification.getActorId() != null) {
            userRepository.findById(notification.getActorId())
                    .ifPresent(actor -> dto.setActorUsername(actor.getUsername()));
        }

        if (notification.getEntityType() != null && notification.getEntityId() != null) {
            try {
                Long entityId = Long.parseLong(notification.getEntityId());
                switch (notification.getEntityType()) {
                    case ORDER:
                    case BOOKING:
                        orderRepository.findById(entityId)
                                .ifPresent(order -> dto.setEntityTitle("订单 " + order.getOrderNumber()));
                        break;
                    case HOMESTAY:
                        homestayRepository.findById(entityId)
                                .ifPresent(homestay -> dto.setEntityTitle(homestay.getTitle()));
                        break;
                    case REVIEW:
                        reviewRepository.findHomestayTitleByReviewId(entityId)
                                .ifPresent(dto::setEntityTitle);
                        break;
                    case USER:
                        userRepository.findById(entityId)
                                .ifPresent(user -> dto.setEntityTitle(user.getUsername()));
                        break;
                    default:
                        log.debug("未处理的实体类型: {}", notification.getEntityType());
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析entityId '{}' 对于通知ID {}: {}",
                        notification.getEntityId(), notification.getId(), e.getMessage());
            }
        }

        return dto;
    }
} 