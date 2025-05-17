package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import com.homestay3.homestaybackend.dto.UpdateReviewRequest;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, exposedHeaders = {"Content-Type", "Content-Length", "Authorization", "Access-Control-Allow-Origin", "Access-Control-Allow-Headers", "Access-Control-Allow-Methods"}, allowCredentials = "true", maxAge = 3600)
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    private final ReviewService reviewService;

    /**
     * 获取用户的评价列表
     */
    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER', 'HOST')")
    public ResponseEntity<?> getUserReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String username = authentication.getName();
        logger.info("获取用户评价列表, 用户: {}", username);
        
        Page<ReviewDTO> reviews = reviewService.getReviewsByUser(username, page, size);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 获取特定房源的评价列表
     */
    @GetMapping("/homestay/{homestayId}")
    public ResponseEntity<?> getHomestayReviews(
            @PathVariable Long homestayId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("获取房源评价列表, 房源ID: {}", homestayId);
        
        Page<ReviewDTO> reviews = reviewService.getReviewsByHomestay(homestayId, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("content", reviews.getContent());
        response.put("totalElements", reviews.getTotalElements());
        response.put("totalPages", reviews.getTotalPages());
        response.put("page", reviews.getNumber());
        response.put("size", reviews.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取房源的评价统计信息
     */
    @GetMapping("/homestay/{homestayId}/stats")
    public ResponseEntity<?> getHomestayReviewStats(@PathVariable Long homestayId) {
        logger.info("获取房源评价统计信息, 房源ID: {}", homestayId);
        
        Map<String, Object> stats = reviewService.getHomestayReviewStats(homestayId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 提交评价
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'HOST')")
    public ResponseEntity<?> submitReview(
            @RequestBody ReviewDTO reviewDTO,
            Authentication authentication) {
        
        String username = authentication.getName();
        logger.info("提交评价, 用户: {}, 房源ID: {}", username, reviewDTO.getHomestayId());
        
        ReviewDTO savedReview = reviewService.submitReview(reviewDTO, username);
        return ResponseEntity.ok(savedReview);
    }
    
    /**
     * 房东回复评价
     */
    @PostMapping("/{reviewId}/response")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> respondToReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> response,
            Authentication authentication) {
        
        String responseText = response.get("response");
        if (responseText == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "回复内容不能为空"));
        }
        
        String username = authentication.getName();
        logger.info("房东回复评价, 房东: {}, 评价ID: {}", username, reviewId);
        
        ReviewDTO updatedReview = reviewService.respondToReview(reviewId, responseText, username);
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * 删除评价（用户本人或管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Service 层会做详细权限检查 (本人或管理员)
    public ResponseEntity<?> deleteReview(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        logger.info("接收到删除评价请求, 评价ID: {}, 请求用户: {}", id, username);
        try {
            reviewService.deleteReview(id);
            logger.info("评价删除成功, ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (ResourceNotFoundException e) {
            logger.warn("删除评价失败 - 未找到资源, ID: {}, 错误: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("删除评价失败 - 权限不足, ID: {}, 用户: {}, 错误: {}", id, username, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("删除评价时发生内部错误, ID: {}, 用户: {}, 错误: {}", id, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "删除评价时发生错误"));
        }
    }

    /**
     * 更新评价（用户本人）
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()") // 确认用户已登录，详细权限检查在 Service 层
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest updateRequest, // 启用 DTO 验证
            Authentication authentication) {
        
        String username = authentication.getName();
        logger.info("接收到更新评价请求, 评价ID: {}, 请求用户: {}", reviewId, username);
        
        try {
            ReviewDTO updatedReview = reviewService.updateReview(reviewId, updateRequest, username);
            logger.info("评价更新成功, ID: {}", reviewId);
            return ResponseEntity.ok(updatedReview);
        } catch (ResourceNotFoundException e) {
            logger.warn("更新评价失败 - 未找到资源, ID: {}, 错误: {}", reviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("更新评价失败 - 权限不足, ID: {}, 用户: {}, 错误: {}", reviewId, username, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) { // 可以考虑捕获 ValidationException 等更具体的异常
            logger.error("更新评价时发生内部错误, ID: {}, 用户: {}, 错误: {}", reviewId, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "更新评价时发生错误"));
        }
    }

    /**
     * 获取房东名下所有房源的评价列表（带筛选和分页）
     * @param authentication 当前认证用户信息
     * @param homestayId 可选，按房源ID筛选
     * @param rating 可选，按评分筛选
     * @param replyStatus 可选，按回复状态筛选 (e.g., "REPLIED", "PENDING")
     * @param pageable 分页和排序信息
     * @return 评价列表分页数据
     */
    @GetMapping("/host")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<Page<ReviewDTO>> getHostReviews(
            Authentication authentication,
            @RequestParam(required = false) Long homestayId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String replyStatus,
            @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String hostUsername = authentication.getName();
        logger.info("获取房东评价列表, 房东: {}, 筛选条件: homestayId={}, rating={}, replyStatus={}", 
                    hostUsername, homestayId, rating, replyStatus);
        
        // 调用 Service 层方法获取数据
        Page<ReviewDTO> reviews = reviewService.getHostReviews(hostUsername, homestayId, rating, replyStatus, pageable);
        
        return ResponseEntity.ok(reviews);
    }

    /**
     * 获取房东的评价统计信息
     * @param authentication 当前认证用户信息
     * @return 评价统计数据 Map
     */
    @GetMapping("/host/stats")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<Map<String, Object>> getHostReviewStats(Authentication authentication) {
        String hostUsername = authentication.getName();
        logger.info("获取房东评价统计信息, 房东: {}", hostUsername);
        
        Map<String, Object> stats = reviewService.getHostReviewStats(hostUsername);
        return ResponseEntity.ok(stats);
    }

    /**
     * 管理员获取所有评价列表（带筛选和分页）
     * @param rating 可选，按评分筛选
     * @param status 可选，按回复状态筛选 ("RESPONDED" 或 "UNREPLIED")
     * @param pageable 分页和排序信息
     * @return 评价列表分页数据
     */
    @GetMapping("/admin") // Use /admin subpath for clarity
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReviewDTO>> getAdminReviews(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String status, // Parameter name matches ReviewService
            @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.info("管理员获取评价列表, 筛选条件: rating={}, status={}", rating, status);
        
        Page<ReviewDTO> reviews = reviewService.getAdminReviews(pageable, rating, status);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 设置评价的可见性 (管理员操作)
     * @param id 评价ID
     * @param visibilityMap 请求体，包含一个 boolean 类型的 'visible' 字段
     * @return 成功则返回 No Content
     */
    @PatchMapping("/{id}/visibility") // 使用 PATCH 更符合部分更新的语义
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setReviewVisibility(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> visibilityMap) {
        
        Boolean isVisible = visibilityMap.get("visible");
        if (isVisible == null) {
            logger.warn("设置评价可见性失败 - 请求体缺少 'visible' 字段, ID: {}", id);
            return ResponseEntity.badRequest().body(Map.of("error", "请求体必须包含 'visible' 字段 (true/false)"));
        }

        logger.info("管理员设置评价可见性, ID: {}, 设置为: {}", id, isVisible);
        
        try {
            reviewService.setReviewVisibility(id, isVisible);
            logger.info("评价可见性设置成功, ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (ResourceNotFoundException e) {
            logger.warn("设置评价可见性失败 - 未找到评价, ID: {}, 错误: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("设置评价可见性时发生内部错误, ID: {}, 错误: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "设置评价可见性时发生错误"));
        }
    }

    /**
     * 删除房东对评价的回复
     * @param reviewId 评价ID
     * @param authentication 当前认证用户信息
     * @return 成功则返回 No Content
     */
    @DeleteMapping("/{reviewId}/response")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')") // 确保是房东角色
    public ResponseEntity<?> deleteReviewResponse(
            @PathVariable Long reviewId,
            Authentication authentication) {
        
        String hostUsername = authentication.getName();
        logger.info("房东 {} 请求删除评价 {} 的回复", hostUsername, reviewId);
        
        try {
            reviewService.deleteReviewResponse(reviewId, hostUsername);
            logger.info("评价 {} 的回复删除成功", reviewId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (ResourceNotFoundException e) {
            logger.warn("删除评价回复失败 - 评价未找到, ID: {}, 错误: {}", reviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("删除评价回复失败 - 权限不足, ID: {}, 房东: {}, 错误: {}", reviewId, hostUsername, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("删除评价回复时发生内部错误, ID: {}, 房东: {}, 错误: {}", reviewId, hostUsername, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "删除回复时发生错误"));
        }
    }
} 