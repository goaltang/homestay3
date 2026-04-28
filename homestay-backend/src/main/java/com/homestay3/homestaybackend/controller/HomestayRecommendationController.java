package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HomestaySummaryDTO;
import com.homestay3.homestaybackend.dto.PagedResponse;
import com.homestay3.homestaybackend.dto.UserRecommendationRequest;
import com.homestay3.homestaybackend.service.HomestayRecommendationService;
import com.homestay3.homestaybackend.util.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 民宿推荐控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class HomestayRecommendationController {

    private final HomestayRecommendationService recommendationService;
    @Autowired
    public HomestayRecommendationController(HomestayRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 获取热门民宿（支持分页）
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularHomestays(
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        try {
            // 如果提供了分页参数，使用分页查询
            if (page != null && size != null) {
                Pageable pageable = PageRequest.of(page, size);
                PagedResponse<HomestaySummaryDTO> popularHomestaysPagedResponse = recommendationService.getPopularHomestaysPage(pageable);
                // 转换为标准的Page对象返回给前端
                Page<HomestaySummaryDTO> popularHomestaysPage = popularHomestaysPagedResponse.toPage(pageable);
                return ResponseEntity.ok(popularHomestaysPage);
            } else {
                // 否则使用原有的limit查询
                List<HomestaySummaryDTO> popularHomestays = recommendationService.getPopularHomestays(limit);
                return ResponseEntity.ok(popularHomestays);
            }
        } catch (Exception e) {
            log.error("获取热门民宿失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取推荐民宿（支持分页）
     */
    @GetMapping("/recommended")
    public ResponseEntity<?> getRecommendedHomestays(
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        try {
            // 如果提供了分页参数，使用分页查询
            if (page != null && size != null) {
                Pageable pageable = PageRequest.of(page, size);
                PagedResponse<HomestaySummaryDTO> recommendedHomestaysPagedResponse = recommendationService.getRecommendedHomestaysPage(pageable);
                // 转换为标准的Page对象返回给前端
                Page<HomestaySummaryDTO> recommendedHomestaysPage = recommendedHomestaysPagedResponse.toPage(pageable);
                return ResponseEntity.ok(recommendedHomestaysPage);
            } else {
                // 否则使用原有的limit查询
                List<HomestaySummaryDTO> recommendedHomestays = recommendationService.getRecommendedHomestays(limit);
                return ResponseEntity.ok(recommendedHomestays);
            }
        } catch (Exception e) {
            log.error("获取推荐民宿失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取个性化推荐民宿
     */
    @GetMapping("/personalized/me")
    public ResponseEntity<List<HomestaySummaryDTO>> getMyPersonalizedRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            Long currentUserId = UserUtil.getCurrentUserId(authentication);
            List<HomestaySummaryDTO> personalizedHomestays = recommendationService.getPersonalizedRecommendations(
                    currentUserId, limit);
            return ResponseEntity.ok(personalizedHomestays);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("获取当前用户个性化推荐民宿失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取个性化推荐民宿
     */
    @GetMapping("/personalized/{userId}")
    public ResponseEntity<List<HomestaySummaryDTO>> getPersonalizedRecommendations(
            @PathVariable Long userId,
            Authentication authentication,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            Long currentUserId = UserUtil.getCurrentUserId(authentication);
            if (!currentUserId.equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            List<HomestaySummaryDTO> personalizedHomestays = recommendationService.getPersonalizedRecommendations(userId, limit);
            return ResponseEntity.ok(personalizedHomestays);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("获取个性化推荐民宿失败，用户ID: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取基于位置的推荐民宿
     */
    @GetMapping("/location")
    public ResponseEntity<List<HomestaySummaryDTO>> getLocationBasedRecommendations(
            @RequestParam String provinceCode,
            @RequestParam String cityCode,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            List<HomestaySummaryDTO> locationHomestays = recommendationService.getLocationBasedRecommendations(
                    provinceCode, cityCode, limit);
            return ResponseEntity.ok(locationHomestays);
        } catch (Exception e) {
            log.error("获取基于位置的推荐民宿失败，省份: {}, 城市: {}", provinceCode, cityCode, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取相似民宿推荐
     */
    @GetMapping("/similar/{homestayId}")
    public ResponseEntity<List<HomestaySummaryDTO>> getSimilarHomestays(
            @PathVariable Long homestayId,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            List<HomestaySummaryDTO> similarHomestays = recommendationService.getSimilarHomestays(homestayId, limit);
            return ResponseEntity.ok(similarHomestays);
        } catch (Exception e) {
            log.error("获取相似民宿推荐失败，民宿ID: {}", homestayId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据用户请求获取推荐
     */
    @PostMapping("/custom")
    public ResponseEntity<List<HomestaySummaryDTO>> getRecommendationsByRequest(
            @RequestBody UserRecommendationRequest request,
            Authentication authentication) {
        try {
            if (request.getUserId() != null
                    || request.getRecommendationType() == UserRecommendationRequest.RecommendationType.PERSONALIZED) {
                Long currentUserId = UserUtil.getCurrentUserId(authentication);
                if (request.getUserId() == null) {
                    request.setUserId(currentUserId);
                } else if (!currentUserId.equals(request.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            List<HomestaySummaryDTO> customRecommendations = recommendationService.getRecommendationsByRequest(request);
            return ResponseEntity.ok(customRecommendations);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("根据用户请求获取推荐失败: {}", request, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 刷新推荐缓存
     */
    @PostMapping("/refresh-cache")
    public ResponseEntity<String> refreshRecommendationCache() {
        try {
            recommendationService.refreshRecommendationCache();
            return ResponseEntity.ok("推荐缓存已刷新");
        } catch (Exception e) {
            log.error("刷新推荐缓存失败", e);
            return ResponseEntity.internalServerError().body("刷新缓存失败");
        }
    }

} 
