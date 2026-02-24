package com.homestay3.homestaybackend.dto;

import java.time.LocalDateTime;

public class UserFavoriteDTO {
    
    private Long id;
    private Long userId;
    private Long homestayId;
    private LocalDateTime createdAt;
    private HomestayDTO homestay; // 可选，用于获取收藏列表时包含民宿信息
    
    public UserFavoriteDTO() {}
    
    public UserFavoriteDTO(Long id, Long userId, Long homestayId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.homestayId = homestayId;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getHomestayId() {
        return homestayId;
    }
    
    public void setHomestayId(Long homestayId) {
        this.homestayId = homestayId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public HomestayDTO getHomestay() {
        return homestay;
    }
    
    public void setHomestay(HomestayDTO homestay) {
        this.homestay = homestay;
    }
} 