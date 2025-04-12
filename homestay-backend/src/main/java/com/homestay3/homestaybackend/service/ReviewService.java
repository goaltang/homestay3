package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.ReviewDTO;
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
     * 根据ID获取评价详情
     */
    ReviewDTO getReviewById(Long id);
    
    /**
     * 删除评价
     */
    void deleteReview(Long id);
    
    /**
     * 设置评价可见性
     */
    void setReviewVisibility(Long id, boolean isVisible);
    
    /**
     * 获取评价统计数据（管理员）
     */
    Map<String, Object> getAdminReviewStats();
} 