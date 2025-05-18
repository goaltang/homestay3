package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.dto.UpdateReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ReviewService {
    
    /**
     * 根据用户名获取用户发表的评价
     */
    Page<ReviewDTO> getReviewsByUser(String username, int page, int size);
    
    /**
     * 根据房源ID获取该房源的所有评价
     */
    Page<ReviewDTO> getReviewsByHomestay(Long homestayId, int page, int size);
    
    /**
     * 获取房源评价统计信息
     */
    Map<String, Object> getHomestayReviewStats(Long homestayId);
    
    /**
     * 提交新评价
     */
    ReviewDTO submitReview(ReviewDTO reviewDTO, String username);
    
    /**
     * 房东回复评价
     */
    ReviewDTO respondToReview(Long reviewId, String response, String username);
    
    /**
     * 管理员获取所有评价（可筛选）
     */
    Page<ReviewDTO> getAdminReviews(Pageable pageable, Integer rating, String status);
    
    /**
     * 获取房东名下所有房源的评价列表（带筛选和分页）
     * @param hostUsername 房东用户名
     * @param homestayId 可选，按房源ID筛选
     * @param rating 可选，按评分筛选
     * @param replyStatus 可选，按回复状态筛选 ("REPLIED" 或 "PENDING")
     * @param pageable 分页和排序信息
     * @return 评价列表分页数据
     */
    Page<ReviewDTO> getHostReviews(String hostUsername, Long homestayId, Integer rating, String replyStatus, Pageable pageable);
    
    /**
     * 获取房东的评价统计信息
     * @param hostUsername 房东用户名
     * @return 包含统计信息的 Map
     */
    Map<String, Object> getHostReviewStats(String hostUsername);
    
    /**
     * 根据ID获取评价详情
     */
    ReviewDTO getReviewById(Long id);
    
    /**
     * 删除评价
     */
    void deleteReview(Long id);
    
    /**
     * 用户更新自己的评价
     * @param reviewId 评价ID
     * @param updateRequest 更新请求的数据
     * @param username 请求更新的用户名
     * @return 更新后的评价 DTO
     */
    ReviewDTO updateReview(Long reviewId, UpdateReviewRequest updateRequest, String username);
    
    /**
     * 设置评价可见性
     */
    void setReviewVisibility(Long id, boolean isVisible);
    
    /**
     * 获取评价统计数据（管理员）
     */
    Map<String, Object> getAdminReviewStats();

    /**
     * 删除房东对评价的回复
     * @param reviewId 评价ID
     * @param hostUsername 操作的房东用户名
     * @throws ResourceNotFoundException 如果评价未找到
     * @throws AccessDeniedException 如果房东无权操作
     */
    void deleteReviewResponse(Long reviewId, String hostUsername);
} 