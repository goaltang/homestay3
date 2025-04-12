package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
} 