package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.dto.NotificationCreateCommand;
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
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
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
    private final WebSocketNotificationService webSocketNotificationService;

// 使用构造函数注入所有依赖
    // 添加 @Lazy 防止潜在的循环依赖 (例如 A -> B -> Notification -> A)
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                    UserRepository userRepository,
                                    @Lazy HomestayRepository homestayRepository,
                                    @Lazy OrderRepository orderRepository,
                                    @Lazy ReviewRepository reviewRepository,
                                    WebSocketNotificationService webSocketNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Override
    @Transactional // 确保操作的原子性
    public Notification createNotification(Long userId, Long actorId, NotificationType type, EntityType entityType, String entityId, String content) {
        return createNotification(NotificationCreateCommand.of(userId, actorId, type, entityType, entityId, content));
    }

    @Override
    @Transactional
    public Notification createNotification(NotificationCreateCommand command) {
        NotificationType normalizedType = normalizeTypeForCreate(command.type());
        EntityType normalizedEntityType = normalizeEntityTypeForCreate(command.entityType());
        Notification notification = Notification.builder()
                .userId(command.userId())
                .actorId(command.actorId())
                .type(normalizedType)
                .entityType(normalizedEntityType)
                .entityId(command.entityId())
                .content(command.content())
                .isRead(false) // 默认为未读
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        if (command.type() != null && command.type().isLegacyAlias()) {
            log.warn("Legacy notification type {} normalized to {} before save", command.type(), normalizedType);
        }
        if (command.entityType() != null && command.entityType().isLegacyAlias()) {
            log.warn("Legacy notification entity type {} normalized to {} before save", command.entityType(), normalizedEntityType);
        }
        log.info("创建通知成功: id={}, userId={}, type={}", savedNotification.getId(), command.userId(), normalizedType);

        // --- 实时推送 ---
        try {
            NotificationDTO dto = convertToDtoWithFullData(savedNotification);
            webSocketNotificationService.sendNotificationToUser(command.userId(), dto);
            log.info("已通过 WebSocket 推送通知给用户: {}", command.userId());
        } catch (Exception e) {
            log.error("WebSocket 推送通知失败: userId={}, error={}", command.userId(), e.getMessage(), e);
            // 推送失败不应影响主流程
        }
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

        // --- 实时推送 ---
        // 如果标记了任何通知为已读，可以推送更新后的未读计数
        if (updatedCount > 0) {
            try {
                long newUnreadCount = getUnreadNotificationCount(userId);
                webSocketNotificationService.sendUnreadCountToUser(userId, newUnreadCount);
            } catch (Exception e) {
                log.error("WebSocket 推送未读计数失败: userId={}, error={}", userId, e.getMessage(), e);
            }
        }
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
        Set<Long> recipientIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        Set<Long> homestayIds = new HashSet<>();
        Set<Long> reviewIds = new HashSet<>();
        Set<Long> userIdsForEntity = new HashSet<>();

        for (Notification notification : notifications) {
            if (notification.getUserId() != null) {
                recipientIds.add(notification.getUserId());
            }

            // 收集actorId
            if (notification.getActorId() != null) {
                actorIds.add(notification.getActorId());
            }

            // 收集entityId根据类型
            if (requiresNumericEntityId(notification.getEntityType()) && notification.getEntityId() != null) {
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

        Map<Long, User> recipientMap = recipientIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(recipientIds).stream()
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
            dtos.add(convertToDtoWithMaps(notification, actorMap, recipientMap, orderMap, homestayMap, reviewHomestayTitleMap, userEntityMap));
        }

        return dtos;
    }

    /**
     * 使用预加载的映射数据转换单个通知
     */
    private NotificationDTO convertToDtoWithMaps(
            Notification notification,
            Map<Long, User> actorMap,
            Map<Long, User> recipientMap,
            Map<Long, Order> orderMap,
            Map<Long, Homestay> homestayMap,
            Map<Long, String> reviewHomestayTitleMap,
            Map<Long, User> userEntityMap) {

        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setActorId(notification.getActorId());
        dto.setType(notification.getType());
        dto.setRawType(getRawType(notification));
        dto.setEntityType(notification.getEntityType());
        dto.setRawEntityType(getRawEntityType(notification));
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
        if (requiresNumericEntityId(notification.getEntityType()) && notification.getEntityId() != null) {
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

        applyPresentationMetadata(dto, recipientMap.get(notification.getUserId()));
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
        dto.setRawType(getRawType(notification));
        dto.setEntityType(notification.getEntityType());
        dto.setRawEntityType(getRawEntityType(notification));
        dto.setEntityId(notification.getEntityId());
        dto.setContent(notification.getContent());
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());

        if (notification.getActorId() != null) {
            userRepository.findById(notification.getActorId())
                    .ifPresent(actor -> dto.setActorUsername(actor.getUsername()));
        }

        if (requiresNumericEntityId(notification.getEntityType()) && notification.getEntityId() != null) {
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

        User recipient = null;
        if (notification.getUserId() != null) {
            recipient = userRepository.findById(notification.getUserId()).orElse(null);
        }
        applyPresentationMetadata(dto, recipient);
        return dto;
    }

    private String getRawType(Notification notification) {
        return notification.getRawType() != null
                ? notification.getRawType()
                : notification.getType() != null ? notification.getType().name() : null;
    }

    private void applyPresentationMetadata(NotificationDTO dto, User recipient) {
        dto.setCategory(resolveCategory(dto.getType()));
        dto.setTitle(resolveTitle(dto.getType()));
        dto.setDeepLink(resolveDeepLink(dto, recipient));
        dto.setPayload(buildPayload(dto));
    }

    private String resolveCategory(NotificationType type) {
        NotificationType canonical = type != null ? type.canonicalType() : NotificationType.UNKNOWN;
        return canonical.getDomain().getCategory();
    }

    private String resolveTitle(NotificationType type) {
        NotificationType canonical = type != null ? type.canonicalType() : NotificationType.UNKNOWN;
        return canonical.getDefaultTitle();
    }

    private String resolveDeepLink(NotificationDTO dto, User recipient) {
        EntityType entityType = dto.getEntityType();
        String entityId = dto.getEntityId();
        boolean hostContext = isHostRecipient(recipient);

        if (entityType == null) {
            return resolveFallbackDeepLink(dto.getType(), hostContext);
        }

        return switch (entityType) {
            case ORDER, BOOKING -> entityId == null ? null :
                    hostContext ? "/host/orders?highlightOrderId=" + entityId : "/orders/" + entityId;
            case HOMESTAY -> entityId == null ? null :
                    hostContext ? "/host/homestay/edit/" + entityId : "/homestays/" + entityId;
            case REVIEW -> entityId == null ? null :
                    hostContext ? "/host/reviews?highlightReviewId=" + entityId : "/user/reviews";
            case MESSAGE, MESSAGE_THREAD -> hostContext ? "/host/messages" : "/user/notifications";
            case USER -> "/user/profile";
            case COUPON -> "/user/coupons";
            case SYSTEM, UNKNOWN -> resolveFallbackDeepLink(dto.getType(), hostContext);
        };
    }

    private String resolveFallbackDeepLink(NotificationType type, boolean hostContext) {
        NotificationType canonicalType = type != null ? type.canonicalType() : NotificationType.UNKNOWN;
        return switch (canonicalType) {
            case NEW_MESSAGE -> hostContext ? "/host/messages" : "/user/notifications";
            case COUPON_EXPIRING, COUPON_ISSUED -> "/user/coupons";
            case HOMESTAY_APPROVED, HOMESTAY_REJECTED, HOMESTAY_SUBMITTED -> "/host/homestay";
            default -> null;
        };
    }

    private boolean isHostRecipient(User recipient) {
        if (recipient == null || recipient.getRole() == null) {
            return false;
        }
        String role = recipient.getRole();
        return role.contains("HOST") || role.contains("ADMIN");
    }

    private Map<String, Object> buildPayload(NotificationDTO dto) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfNotNull(payload, "rawType", dto.getRawType());
        putIfNotNull(payload, "rawEntityType", dto.getRawEntityType());
        putIfNotNull(payload, "entityType", dto.getEntityType() != null ? dto.getEntityType().name() : null);
        putIfNotNull(payload, "entityId", dto.getEntityId());
        putIfNotNull(payload, "entityTitle", dto.getEntityTitle());
        putIfNotNull(payload, "actorId", dto.getActorId());
        putIfNotNull(payload, "actorUsername", dto.getActorUsername());
        return payload;
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private boolean requiresNumericEntityId(EntityType entityType) {
        if (entityType == null) {
            return false;
        }
        return switch (entityType) {
            case ORDER, BOOKING, HOMESTAY, REVIEW, USER -> true;
            case MESSAGE, MESSAGE_THREAD, SYSTEM, COUPON, UNKNOWN -> false;
        };
    }

    private String getRawEntityType(Notification notification) {
        return notification.getRawEntityType() != null
                ? notification.getRawEntityType()
                : notification.getEntityType() != null ? notification.getEntityType().name() : null;
    }

    private NotificationType normalizeTypeForCreate(NotificationType type) {
        return type == null ? NotificationType.UNKNOWN : type.canonicalType();
    }

    private EntityType normalizeEntityTypeForCreate(EntityType entityType) {
        return entityType == null ? null : entityType.canonicalType();
    }
} 
