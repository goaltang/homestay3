package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.UserFavoriteDTO;

import java.util.List;

public interface UserFavoriteService {
    
    /**
     * 添加收藏
     */
    UserFavoriteDTO addFavorite(Long userId, Long homestayId);
    
    /**
     * 取消收藏
     */
    void removeFavorite(Long userId, Long homestayId);
    
    /**
     * 检查是否已收藏
     */
    boolean isFavorite(Long userId, Long homestayId);
    
    /**
     * 获取用户收藏的民宿列表
     */
    List<HomestayDTO> getUserFavoriteHomestays(Long userId);
    
    /**
     * 获取用户收藏的民宿ID列表
     */
    List<Long> getUserFavoriteHomestayIds(Long userId);
    
    /**
     * 获取用户的收藏记录列表
     */
    List<UserFavoriteDTO> getUserFavorites(Long userId);
    
    /**
     * 清空用户所有收藏
     */
    void clearUserFavorites(Long userId);
    
    /**
     * 获取用户收藏数量
     */
    long getUserFavoriteCount(Long userId);
    
    /**
     * 获取民宿被收藏的次数
     */
    long getHomestayFavoriteCount(Long homestayId);
    
    /**
     * 批量检查收藏状态
     */
    List<Boolean> checkFavoriteStatus(Long userId, List<Long> homestayIds);
} 