package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        Homestay homestay = homestayRepository.findById(reviewDTO.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在: " + reviewDTO.getHomestayId()));
        
        Review review = Review.builder()
                .user(user)
                .homestay(homestay)
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .cleanlinessRating(reviewDTO.getCleanlinessRating())
                .accuracyRating(reviewDTO.getAccuracyRating())
                .communicationRating(reviewDTO.getCommunicationRating())
                .locationRating(reviewDTO.getLocationRating())
                .checkInRating(reviewDTO.getCheckInRating())
                .valueRating(reviewDTO.getValueRating())
                .isPublic(true)
                .build();
        
        Review savedReview = reviewRepository.save(review);
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
    public ReviewDTO getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评价不存在: " + id));
        return convertToDTO(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("评价不存在: " + id);
        }
        reviewRepository.deleteById(id);
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

    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
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
    }
} 