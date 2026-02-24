package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.ApiResponse;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.UserFavoriteDTO;
import com.homestay3.homestaybackend.service.UserFavoriteService;
import com.homestay3.homestaybackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class UserFavoriteController {

    private final UserFavoriteService userFavoriteService;
    private final UserService userService;

    @Autowired
    public UserFavoriteController(UserFavoriteService userFavoriteService, UserService userService) {
        this.userFavoriteService = userFavoriteService;
        this.userService = userService;
    }

    /**
     * 添加收藏
     */
    @PostMapping("/{homestayId}")
    public ResponseEntity<ApiResponse<UserFavoriteDTO>> addFavorite(
            @PathVariable Long homestayId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            UserFavoriteDTO favorite = userFavoriteService.addFavorite(userId, homestayId);
            return ResponseEntity.ok(ApiResponse.success(favorite, "添加收藏成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("添加收藏失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("添加收藏失败"));
        }
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{homestayId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long homestayId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            userFavoriteService.removeFavorite(userId, homestayId);
            return ResponseEntity.ok(ApiResponse.success(null, "取消收藏成功"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("取消收藏失败"));
        }
    }

    /**
     * 切换收藏状态
     */
    @PostMapping("/toggle/{homestayId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(
            @PathVariable Long homestayId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            boolean isFavorite = userFavoriteService.isFavorite(userId, homestayId);
            
            if (isFavorite) {
                userFavoriteService.removeFavorite(userId, homestayId);
                return ResponseEntity.ok(ApiResponse.success(
                    Map.of("isFavorite", false, "action", "removed"), 
                    "取消收藏成功"));
            } else {
                UserFavoriteDTO favorite = userFavoriteService.addFavorite(userId, homestayId);
                return ResponseEntity.ok(ApiResponse.success(
                    Map.of("isFavorite", true, "action", "added", "favorite", favorite), 
                    "添加收藏成功"));
            }
        } catch (Exception e) {
            log.error("切换收藏状态失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("操作失败"));
        }
    }

    /**
     * 检查收藏状态
     */
    @GetMapping("/check/{homestayId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @PathVariable Long homestayId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            boolean isFavorite = userFavoriteService.isFavorite(userId, homestayId);
            return ResponseEntity.ok(ApiResponse.success(isFavorite));
        } catch (Exception e) {
            log.error("检查收藏状态失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("检查收藏状态失败"));
        }
    }

    /**
     * 获取用户收藏的民宿列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HomestayDTO>>> getUserFavorites(
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<HomestayDTO> favorites = userFavoriteService.getUserFavoriteHomestays(userId);
            return ResponseEntity.ok(ApiResponse.success(favorites));
        } catch (Exception e) {
            log.error("获取用户收藏列表失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取收藏列表失败"));
        }
    }

    /**
     * 获取用户收藏的民宿ID列表
     */
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Long>>> getUserFavoriteIds(
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<Long> favoriteIds = userFavoriteService.getUserFavoriteHomestayIds(userId);
            return ResponseEntity.ok(ApiResponse.success(favoriteIds));
        } catch (Exception e) {
            log.error("获取用户收藏ID列表失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取收藏ID列表失败"));
        }
    }

    /**
     * 清空用户所有收藏
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearUserFavorites(
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            userFavoriteService.clearUserFavorites(userId);
            return ResponseEntity.ok(ApiResponse.success(null, "清空收藏成功"));
        } catch (Exception e) {
            log.error("清空用户收藏失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("清空收藏失败"));
        }
    }

    /**
     * 获取用户收藏数量
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserFavoriteCount(
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            long count = userFavoriteService.getUserFavoriteCount(userId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("获取用户收藏数量失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取收藏数量失败"));
        }
    }

    /**
     * 批量检查收藏状态
     */
    @PostMapping("/check-batch")
    public ResponseEntity<ApiResponse<List<Boolean>>> checkFavoritesStatus(
            @RequestBody List<Long> homestayIds,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<Boolean> statuses = userFavoriteService.checkFavoriteStatus(userId, homestayIds);
            return ResponseEntity.ok(ApiResponse.success(statuses));
        } catch (Exception e) {
            log.error("批量检查收藏状态失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("批量检查收藏状态失败"));
        }
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        String username = authentication.getName();
        return userService.getUserByUsername(username).getId();
    }
} 