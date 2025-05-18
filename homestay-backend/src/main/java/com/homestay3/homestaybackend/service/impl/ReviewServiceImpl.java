package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.dto.UpdateReviewRequest;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.ReviewService;
import com.homestay3.homestaybackend.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Override
    public Page<ReviewDTO> getReviewsByUser(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return reviewRepository.findByUser_Id(user.getId(), pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ReviewDTO> getReviewsByHomestay(Long homestayId, int page, int size) {
        if (!homestayRepository.existsById(homestayId)) {
            throw new ResourceNotFoundException("房源不存在: " + homestayId);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return reviewRepository.findByHomestay_IdAndIsPublicTrue(homestayId, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Map<String, Object> getHomestayReviewStats(Long homestayId) {
        // 检查房源是否存在
        if (!homestayRepository.existsById(homestayId)) {
            throw new ResourceNotFoundException("Homestay not found with id: " + homestayId);
        }
        
        Double averageRating = reviewRepository.getAverageRatingByHomestayId(homestayId);
        Long reviewCount = reviewRepository.getReviewCountByHomestayId(homestayId);
        List<Object[]> detailedRatings = reviewRepository.getDetailedRatingsByHomestayId(homestayId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);
        stats.put("reviewCount", reviewCount);
        
        // 处理详细评分
        Map<String, Double> detailedStats = new HashMap<>();
        if (!detailedRatings.isEmpty() && detailedRatings.get(0) != null) {
            Object[] ratings = detailedRatings.get(0);
            detailedStats.put("cleanlinessRating", ratings[0] != null ? (Double) ratings[0] : 0.0);
            detailedStats.put("accuracyRating", ratings[1] != null ? (Double) ratings[1] : 0.0);
            detailedStats.put("communicationRating", ratings[2] != null ? (Double) ratings[2] : 0.0);
            detailedStats.put("locationRating", ratings[3] != null ? (Double) ratings[3] : 0.0);
            detailedStats.put("checkInRating", ratings[4] != null ? (Double) ratings[4] : 0.0);
            detailedStats.put("valueRating", ratings[5] != null ? (Double) ratings[5] : 0.0);
        }
        
        stats.put("detailedRatings", detailedStats);
        
        return stats;
    }

    @Override
    @Transactional
    public ReviewDTO submitReview(ReviewDTO reviewDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        if (reviewDTO.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required to submit a review.");
        }

        // 1. 查找订单
        Order order = orderRepository.findById(reviewDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + reviewDTO.getOrderId()));

        // 2. 验证订单归属
        if (!order.getGuest().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to review this order.");
        }

        // 3. 验证订单状态 (假设 'COMPLETED' 是完成状态)
        // 注意: 您需要根据实际的 OrderStatus 枚举或状态字符串进行调整
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
             throw new IllegalArgumentException("You can only review completed orders.");
             // 或者根据您的 OrderStatus 枚举调整:
             // if (order.getStatus() != OrderStatus.COMPLETED) { ... }
        }


        // 4. 验证订单对应的民宿是否与 DTO 中的 homestayId 一致 (可选但推荐)
        if (!order.getHomestay().getId().equals(reviewDTO.getHomestayId())) {
            throw new IllegalArgumentException("Order does not belong to the specified homestay.");
        }
        
        Homestay homestay = order.getHomestay(); // 直接从订单获取民宿，避免二次查询

        // 5. 检查是否已评论
        if (reviewRepository.existsByOrder(order)) {
            throw new IllegalArgumentException("You have already reviewed this order.");
        }

        // 6. 创建并保存评论
        Review review = Review.builder()
                .user(user)
                .homestay(homestay)
                .order(order) // 设置关联的订单
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .cleanlinessRating(reviewDTO.getCleanlinessRating())
                .accuracyRating(reviewDTO.getAccuracyRating())
                .communicationRating(reviewDTO.getCommunicationRating())
                .locationRating(reviewDTO.getLocationRating())
                .checkInRating(reviewDTO.getCheckInRating())
                .valueRating(reviewDTO.getValueRating())
                .isPublic(true)
                // createTime 会自动设置
                .build();
        
        Review savedReview = reviewRepository.save(review);
        
        // --- 添加通知逻辑 (通知房东) ---
        try {
            User host = homestay.getOwner();
            String notificationContent = String.format(
                    "用户 %s 对您的房源 '%s' 提交了新的评价。",
                    user.getUsername(), // 评价者用户名
                    homestay.getTitle() // 房源标题
            );
            notificationService.createNotification(
                    host.getId(),               // 接收者: 房东
                    user.getId(),               // 触发者: 房客
                    NotificationType.NEW_REVIEW, // 类型: 新评价
                    EntityType.REVIEW,         // 关联实体类型: 评价
                    String.valueOf(savedReview.getId()), // 关联实体ID: 评价ID
                    notificationContent         // 内容
            );
            logger.info("已为房东 {} (房源 {}) 发送新评价通知 (评价 ID: {})", host.getUsername(), homestay.getTitle(), savedReview.getId());
        } catch (Exception e) {
            logger.error("为房东 {} 发送新评价通知失败 (评价 ID: {}): {}",
                    homestay.getOwner().getUsername(), savedReview.getId(), e.getMessage(), e);
            // 发送通知失败不应中断评价提交流程
        }
        // --- 通知逻辑结束 ---

        // 可选：更新民宿的平均分等统计信息
        // updateHomestayReviewStats(homestay.getId()); 
        
        return convertToDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewDTO respondToReview(Long reviewId, String response, String username) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("评价不存在: " + reviewId));
        
        // 检查当前用户是否是该房源的房东
        User host = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        if (!review.getHomestay().getOwner().getId().equals(host.getId())) {
            throw new IllegalStateException("只有房东才能回复评价");
        }
        
        review.setResponse(response);
        review.setResponseTime(LocalDateTime.now());
        
        Review updatedReview = reviewRepository.save(review);

        // --- 添加通知逻辑 (通知房客) ---
        try {
            User guest = review.getUser(); // 获取原始评价者
            String notificationContent = String.format(
                    "房东回复了您对房源 '%s' 的评价。",
                    review.getHomestay().getTitle() // 房源标题
            );
            notificationService.createNotification(
                    guest.getId(),                    // 接收者: 房客
                    host.getId(),                     // 触发者: 房东
                    NotificationType.REVIEW_REPLIED,  // 类型: 评价被回复
                    EntityType.REVIEW,                // 关联实体类型: 评价
                    String.valueOf(updatedReview.getId()), // 关联实体ID: 评价ID
                    notificationContent              // 内容
            );
            logger.info("已为房客 {} 发送评价回复通知 (评价 ID: {})", guest.getUsername(), updatedReview.getId());
        } catch (Exception e) {
            logger.error("为房客 {} 发送评价回复通知失败 (评价 ID: {}): {}",
                    review.getUser().getUsername(), updatedReview.getId(), e.getMessage(), e);
            // 发送通知失败不应中断评价回复流程
        }
        // --- 通知逻辑结束 ---

        return convertToDTO(updatedReview);
    }

    @Override
    public Page<ReviewDTO> getAdminReviews(Pageable pageable, Integer rating, String status) {
        Specification<Review> spec = Specification.where(null);
        
        // 按评分筛选
        if (rating != null) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("rating"), rating));
        }
        
        // 按回复状态筛选
        if (status != null) {
            if ("RESPONDED".equals(status)) {
                spec = spec.and((root, query, criteriaBuilder) -> 
                    criteriaBuilder.isNotNull(root.get("response")));
            } else if ("UNREPLIED".equals(status)) {
                spec = spec.and((root, query, criteriaBuilder) -> 
                    criteriaBuilder.isNull(root.get("response")));
            }
        }
        
        return reviewRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    @Override
    public Page<ReviewDTO> getHostReviews(String hostUsername, Long homestayId, Integer rating, String replyStatus, Pageable pageable) {
        // 1. 获取房东用户实体
        User host = userRepository.findByUsername(hostUsername)
                .orElseThrow(() -> new ResourceNotFoundException("房东用户不存在: " + hostUsername));
        
        // 2. 构建查询条件 (Specification)
        Specification<Review> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 基本条件：评价所属房源的房东是当前用户
            predicates.add(criteriaBuilder.equal(root.get("homestay").get("owner").get("id"), host.getId()));

            // 按房源ID筛选
            if (homestayId != null) {
                predicates.add(criteriaBuilder.equal(root.get("homestay").get("id"), homestayId));
            }

            // 按评分筛选
            if (rating != null) {
                predicates.add(criteriaBuilder.equal(root.get("rating"), rating));
            }

            // 按回复状态筛选
            if (replyStatus != null && !replyStatus.isEmpty()) {
                if ("REPLIED".equalsIgnoreCase(replyStatus)) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("response")));
                } else if ("PENDING".equalsIgnoreCase(replyStatus)) { // 使用 PENDING 或 UNREPLIED 都可以，这里统一用 PENDING
                    predicates.add(criteriaBuilder.isNull(root.get("response")));
                }
                // 可以添加其他状态的处理
            }

            // 添加其他必要的默认条件，例如只查询公开的评价？(根据需求定)
            // predicates.add(criteriaBuilder.isTrue(root.get("isPublic")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 3. 执行查询
        Page<Review> reviewPage = reviewRepository.findAll(spec, pageable);

        // 4. 转换并返回 DTO
        return reviewPage.map(this::convertToDTO);
    }

    @Override
    public Map<String, Object> getHostReviewStats(String hostUsername) {
        // 1. 获取房东用户实体
        User host = userRepository.findByUsername(hostUsername)
                .orElseThrow(() -> new ResourceNotFoundException("房东用户不存在: " + hostUsername));
        Long hostId = host.getId();

        Map<String, Object> stats = new HashMap<>();
        
        // 统计总评价数
        long totalCount = reviewRepository.countByHostId(hostId);
        stats.put("total", totalCount);
        
        // 统计各评分段的评价数
        long goodCount = reviewRepository.countByHostIdAndRatingGreaterThanEqual(hostId, 4);
        long neutralCount = reviewRepository.countByHostIdAndRating(hostId, 3);
        long badCount = reviewRepository.countByHostIdAndRatingLessThanEqual(hostId, 2);
        
        stats.put("good", goodCount);    // 好评 (4-5星)
        stats.put("neutral", neutralCount); // 中评 (3星)
        stats.put("bad", badCount);      // 差评 (1-2星)
        
        // 统计平均评分 (只统计公开的？需要确认，这里暂时不加 isPublic 条件)
        Double avgRating = reviewRepository.getAverageRatingByHostId(hostId);
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        
        // 统计已回复和未回复的评价数
        long respondedCount = reviewRepository.countByHostIdAndResponseIsNotNull(hostId);
        long unrepliedCount = totalCount - respondedCount; // 使用 PENDING 状态
        
        stats.put("responded", respondedCount);
        stats.put("unreplied", unrepliedCount);
        
        logger.info("获取房东 {} 的评价统计: {}", hostUsername, stats);
        return stats;
    }

    @Override
    public ReviewDTO getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评价不存在: " + id));
        return convertToDTO(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        logger.info("Attempting to delete review with id: {}", id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            logger.warn("Unauthorized attempt to delete review {}: User not authenticated", id);
            throw new AccessDeniedException("User not authenticated. Cannot delete review.");
        }

        String currentUsername = authentication.getName();
        logger.debug("User '{}' is attempting to delete review {}", currentUsername, id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Review not found with id: {}", id);
                    return new ResourceNotFoundException("Review not found with id: " + id);
                });

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> {
                     logger.error("Authenticated user '{}' not found in database.", currentUsername);
                     // 通常不应该发生，但以防万一
                     return new IllegalStateException("Authenticated user not found in database.");
                });

        // 检查是否是评论者本人
        boolean isOwner = review.getUser().getId().equals(currentUser.getId());
        logger.debug("Is user '{}' the owner of review {}? {}", currentUsername, id, isOwner);

        // 检查是否是管理员
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals); // 假设管理员角色是 ROLE_ADMIN
        logger.debug("Is user '{}' an admin? {}", currentUsername, isAdmin);


        // 权限判断：必须是评论者本人或管理员
        if (!isOwner && !isAdmin) {
            logger.warn("Access denied for user '{}' trying to delete review {}. User is not owner or admin.", currentUsername, id);
            throw new AccessDeniedException("You do not have permission to delete this review.");
        }

        logger.info("Permission granted. Deleting review {} requested by user '{}' (Is Owner: {}, Is Admin: {})", id, currentUsername, isOwner, isAdmin);
        reviewRepository.delete(review); // 使用 delete(entity) 更安全
        logger.info("Review deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public void setReviewVisibility(Long id, boolean isVisible) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评价不存在: " + id));
        review.setIsPublic(isVisible);
        reviewRepository.save(review);
    }

    @Override
    public Map<String, Object> getAdminReviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计总评价数
        long totalCount = reviewRepository.count();
        stats.put("total", totalCount);
        
        // 统计各评分段的评价数
        long goodCount = reviewRepository.countByRatingGreaterThanEqual(4);
        long neutralCount = reviewRepository.countByRating(3);
        long badCount = reviewRepository.countByRatingLessThanEqual(2);
        
        stats.put("good", goodCount);    // 好评 (4-5星)
        stats.put("neutral", neutralCount); // 中评 (3星)
        stats.put("bad", badCount);      // 差评 (1-2星)
        
        // 统计平均评分
        Double avgRating = reviewRepository.getAverageRating();
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        
        // 统计已回复和未回复的评价数
        long respondedCount = reviewRepository.countByResponseIsNotNull();
        long unrepliedCount = totalCount - respondedCount;
        
        stats.put("responded", respondedCount);
        stats.put("unreplied", unrepliedCount);
        
        return stats;
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(Long reviewId, UpdateReviewRequest updateRequest, String username) {
        logger.info("开始更新评价, ID: {}, 用户: {}", reviewId, username);
        
        // 1. 查找评价
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    logger.warn("更新评价失败 - 未找到评价, ID: {}", reviewId);
                    return new ResourceNotFoundException("评价不存在: " + reviewId);
                });
                
        // 2. 查找请求用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // 这个理论上不应该发生，因为用户已经通过了认证
                    logger.error("更新评价失败 - 请求用户未找到, Username: {}", username);
                    return new ResourceNotFoundException("用户不存在: " + username);
                });

        // 3. 权限验证：确保是评价的作者本人
        if (!review.getUser().getId().equals(user.getId())) {
            logger.warn("更新评价失败 - 权限不足, 评价ID: {}, 请求用户: {}, 评价作者ID: {}", 
                reviewId, username, review.getUser().getId());
            throw new AccessDeniedException("您没有权限修改此评价");
        }

        // 4. 更新评价字段
        review.setRating(updateRequest.getRating());
        review.setContent(updateRequest.getContent());
        review.setUpdateTime(LocalDateTime.now()); // 更新修改时间
        
        // 如果 UpdateReviewRequest 中包含子评分，也在此处更新
        // review.setCleanlinessRating(updateRequest.getCleanlinessRating());
        // ...
        
        // 5. 保存更新
        Review updatedReview = reviewRepository.save(review);
        logger.info("评价更新成功, ID: {}", updatedReview.getId());
        
        // 6. 转换并返回 DTO
        return convertToDTO(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReviewResponse(Long reviewId, String hostUsername) {
        logger.info("房东 {} 请求删除评价 {} 的回复", hostUsername, reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("评价未找到, ID: " + reviewId));

        // 验证房东权限
        Homestay homestay = review.getHomestay();
        if (homestay == null || !homestay.getOwner().getUsername().equals(hostUsername)) {
            logger.warn("房东 {} 无权删除评价 {} 的回复", hostUsername, reviewId);
            throw new AccessDeniedException("您无权删除此评价的回复");
        }

        // 检查是否已有回复
        if (review.getResponse() == null) {
            logger.info("评价 {} 本身没有回复，无需删除", reviewId);
            // 可以选择直接返回或抛出特定异常，这里选择直接返回表示操作完成
            return; 
        }

        // 清除回复内容和时间
        review.setResponse(null);
        review.setResponseTime(null);
        reviewRepository.save(review);

        logger.info("成功删除评价 {} 的回复", reviewId);
    }

    private ReviewDTO convertToDTO(Review review) {
        ReviewDTO dto = ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getUsername())
                .userAvatar(review.getUser().getAvatar())
                .homestayId(review.getHomestay().getId())
                .homestayTitle(review.getHomestay().getTitle())
                .rating(review.getRating())
                .content(review.getContent())
                .cleanlinessRating(review.getCleanlinessRating())
                .accuracyRating(review.getAccuracyRating())
                .communicationRating(review.getCommunicationRating())
                .locationRating(review.getLocationRating())
                .checkInRating(review.getCheckInRating())
                .valueRating(review.getValueRating())
                .response(review.getResponse())
                .responseTime(review.getResponseTime() != null ? review.getResponseTime() : null)
                .createTime(review.getCreateTime())
                .isPublic(review.getIsPublic())
                .build();
        if (review.getOrder() != null) {
             dto.setOrderId(review.getOrder().getId());
         }
         return dto;
    }
} 